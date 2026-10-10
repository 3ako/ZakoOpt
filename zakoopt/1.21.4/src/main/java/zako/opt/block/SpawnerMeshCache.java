package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

// the mob spinning in a spawner is a full entity render per spawner per frame, yet it never moves: it is drawn once in
// its own space and replayed through the spawner's pose, positions and normals, with the light swapped in. Lost: the
// sub-tick bob of animations driven by the partial tick
@UtilityClass
public class SpawnerMeshCache {
	// a packed light vanilla never produces, so emissive parts (spider eyes) keep their own light
	private final int LIGHT_MARK = 0x00370013;
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(16384);
	private final Map<Entity, Mesh> CACHE = new WeakHashMap<>();

	// positions and normals shared by several vertices (a box's 24 corners are 8, its 4 vertices per face share a normal)
	// go through the matrices once, as Sodium's cuboid writer does
	private record Segment(RenderType type, ByteBuffer vertices, int count, float[] positions, int[] positionIndex, int[] normals, int[] normalIndex) {
	}

	private record Mesh(Object renderer, List<Segment> segments) {
	}

	// record draws the entity with an identity pose into the given buffers and light; false: draw it the vanilla way
	public boolean render(Entity entity, EntityRenderDispatcher dispatcher, PoseStack poseStack, MultiBufferSource buffers, int light,
						  BiConsumer<MultiBufferSource, Integer> record) {
		Object renderer = dispatcher.getRenderer(entity);
		Mesh mesh = CACHE.get(entity);
		if (mesh == null || mesh.renderer != renderer) {
			Recorder recorder = new Recorder();
			try {
				record.accept(recorder, LIGHT_MARK);
			} finally {
				recorder.flush();
			}
			mesh = new Mesh(renderer, recorder.segments);
			CACHE.put(entity, mesh);
		}
		return emit(mesh, poseStack.last(), buffers, light);
	}

	private float[] positions = new float[0];
	private int[] normals = new int[0];

	private boolean emit(Mesh mesh, PoseStack.Pose pose, MultiBufferSource buffers, int light) {
		Matrix4f matrix = pose.pose();
		Vector3f tmp = new Vector3f();
		for (int s = 0; s < mesh.segments.size(); s++) {
			Segment segment = mesh.segments.get(s);
			VertexFormat format = segment.type.format();
			VertexConsumer target = buffers.getBuffer(segment.type);
			// ponytail: only the first buffer is checked, an entity's buffers all come from one source
			if (s == 0 && !RawVertices.accepts(target, format)) {
				return false;
			}
			int stride = format.getVertexSize();
			int at = format.getOffset(VertexFormatElement.POSITION);
			int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
			int normal = format.contains(VertexFormatElement.NORMAL) ? format.getOffset(VertexFormatElement.NORMAL) : -1;
			int size = segment.count * stride;
			if (positions.length < segment.positions.length) {
				positions = new float[segment.positions.length];
			}
			for (int p = 0; p < segment.positions.length; p += 3) {
				matrix.transformPosition(segment.positions[p], segment.positions[p + 1], segment.positions[p + 2], tmp);
				positions[p] = tmp.x;
				positions[p + 1] = tmp.y;
				positions[p + 2] = tmp.z;
			}
			if (normals.length < segment.normals.length) {
				normals = new int[segment.normals.length];
			}
			for (int n = 0; n < segment.normals.length; n++) {
				int packed = segment.normals[n];
				pose.transformNormal((byte) packed / 127.0F, (byte) (packed >> 8) / 127.0F, (byte) (packed >> 16) / 127.0F, tmp);
				normals[n] = normal(tmp.x) & 0xFF | (normal(tmp.y) & 0xFF) << 8 | (normal(tmp.z) & 0xFF) << 16 | packed & 0xFF000000;
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				long dst = stack.nmalloc(16, size);
				MemoryUtil.memCopy(MemoryUtil.memAddress(segment.vertices), dst, size);
				int i = 0;
				for (long v = dst, end = dst + size; v < end; v += stride, i++) {
					int p = segment.positionIndex[i] * 3;
					MemoryUtil.memPutFloat(v + at, positions[p]);
					MemoryUtil.memPutFloat(v + at + 4, positions[p + 1]);
					MemoryUtil.memPutFloat(v + at + 8, positions[p + 2]);
					if (uv2 >= 0 && MemoryUtil.memGetInt(v + uv2) == LIGHT_MARK) {
						MemoryUtil.memPutInt(v + uv2, light);
					}
					if (normal >= 0) {
						MemoryUtil.memPutInt(v + normal, normals[segment.normalIndex[i]]);
					}
				}
				RawVertices.push(target, stack, dst, segment.count, format);
			}
		}
		return true;
	}

	private Segment index(RenderType type, ByteBuffer vertices, int count) {
		VertexFormat format = type.format();
		int stride = format.getVertexSize();
		int at = format.getOffset(VertexFormatElement.POSITION);
		int normal = format.contains(VertexFormatElement.NORMAL) ? format.getOffset(VertexFormatElement.NORMAL) : -1;
		Map<List<Float>, Integer> positionIds = new HashMap<>();
		Map<Integer, Integer> normalIds = new HashMap<>();
		List<Float> positionList = new ArrayList<>();
		List<Integer> normalList = new ArrayList<>();
		int[] positionIndex = new int[count];
		int[] normalIndex = new int[count];
		long base = MemoryUtil.memAddress(vertices);
		for (int i = 0; i < count; i++) {
			long v = base + (long) i * stride;
			List<Float> pos = List.of(MemoryUtil.memGetFloat(v + at), MemoryUtil.memGetFloat(v + at + 4), MemoryUtil.memGetFloat(v + at + 8));
			positionIndex[i] = positionIds.computeIfAbsent(pos, k -> {
				positionList.addAll(k);
				return positionList.size() / 3 - 1;
			});
			if (normal >= 0) {
				normalIndex[i] = normalIds.computeIfAbsent(MemoryUtil.memGetInt(v + normal), k -> {
					normalList.add(k);
					return normalList.size() - 1;
				});
			}
		}
		float[] positions = new float[positionList.size()];
		for (int i = 0; i < positions.length; i++) {
			positions[i] = positionList.get(i);
		}
		return new Segment(type, vertices, count, positions, positionIndex, normalList.stream().mapToInt(Integer::intValue).toArray(), normalIndex);
	}

	private byte normal(float c) {
		return (byte) (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F);
	}

	// one segment per run of draws into the same render type, as the vanilla buffer source would see them
	private static final class Recorder implements MultiBufferSource {
		final List<Segment> segments = new ArrayList<>();
		RenderType type;
		BufferBuilder builder;

		@Override
		public VertexConsumer getBuffer(RenderType renderType) {
			if (renderType != type) {
				flush();
				type = renderType;
				builder = new BufferBuilder(SCRATCH, renderType.mode(), renderType.format());
			}
			return builder;
		}

		void flush() {
			if (builder == null) {
				return;
			}
			try (MeshData mesh = builder.build()) {
				if (mesh != null) {
					ByteBuffer src = mesh.vertexBuffer();
					ByteBuffer copy = ByteBuffer.allocateDirect(src.remaining());
					MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(copy), src.remaining());
					segments.add(index(type, copy, mesh.drawState().vertexCount()));
				}
			}
			builder = null;
			type = null;
		}
	}
}
