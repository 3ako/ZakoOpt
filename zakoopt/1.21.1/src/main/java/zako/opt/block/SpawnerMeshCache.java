package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;
import zako.opt.gl.ReserveBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// the mob spinning in a spawner is a full entity render per spawner per frame, yet it never moves: it is drawn once in
// its own space and replayed through the spawner's pose, positions and normals, with the light swapped in. Lost: the
// sub-tick bob of animations driven by the partial tick
@UtilityClass
public class SpawnerMeshCache {
	// a packed light vanilla never produces, so emissive parts (spider eyes) keep their own light
	private final int LIGHT_MARK = 0x00370013;
	private final int CHUNK = 16384;
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(16384);
	private final Map<Entity, Mesh> CACHE = new WeakHashMap<>();

	// positions and normals shared by several vertices (a box's 24 corners are 8, its 4 vertices per face share a normal)
	// go through the matrices once, as Sodium's cuboid writer does
	// recorded under a shader pack, the vertices are in Iris's extended format: never replayed into a vanilla buffer
	public record Segment(RenderType type, VertexFormat format, ByteBuffer vertices, int count, float[] positions, int[] positionIndex, int[] normals, int[] normalIndex) {
	}

	private record Mesh(Object renderer, List<Segment> segments) {
	}

	// record draws the entity with an identity pose into the given buffers and light; false: draw it the vanilla way
	public boolean render(Entity entity, EntityRenderDispatcher dispatcher, PoseStack poseStack, MultiBufferSource buffers, int light,
						  BiConsumer<MultiBufferSource, Integer> record) {
		Object renderer = dispatcher.getRenderer(entity);
		Mesh mesh = CACHE.get(entity);
		if (mesh == null || mesh.renderer != renderer) {
			mesh = new Mesh(renderer, record(recorder -> record.accept(recorder, LIGHT_MARK)));
			CACHE.put(entity, mesh);
		}
		return emit(mesh.segments, poseStack.last(), buffers, light);
	}

	// the draw goes into buffers that keep its vertices, one segment per run of the same render type
	public List<Segment> record(Consumer<MultiBufferSource> draw) {
		return record(draw, false);
	}

	// owned: the vertices live in memAlloc'd memory the caller hands back to free(); the spawner's meshes go with their entity instead
	public List<Segment> record(Consumer<MultiBufferSource> draw, boolean owned) {
		Recorder recorder = new Recorder(owned);
		try {
			draw.accept(recorder);
		} finally {
			recorder.flush();
		}
		return recorder.segments;
	}

	private float[] positions = new float[0];
	private int[] normals = new int[0];

	public boolean emit(List<Segment> segments, PoseStack.Pose pose, MultiBufferSource buffers, int light) {
		Matrix4f matrix = pose.pose();
		Vector3f tmp = new Vector3f();
		// a block entity's pose only moves: positions shift in place and the recorded normals stand as they are
		Matrix3f n3 = pose.normal();
		boolean shift = (matrix.properties() & Matrix4fc.PROPERTY_TRANSLATION) != 0 && n3.m00() == 1.0F && n3.m11() == 1.0F && n3.m22() == 1.0F;
		float tx = matrix.m30(), ty = matrix.m31(), tz = matrix.m32();
		for (int s = 0; s < segments.size(); s++) {
			Segment segment = segments.get(s);
			VertexFormat format = segment.format;
			VertexConsumer target = buffers.getBuffer(segment.type);
			// ponytail: only the first buffer is checked, an entity's buffers all come from one source
			if (s == 0 && (format != segment.type.format() || !RawVertices.accepts(target, format))) {
				return false;
			}
			int stride = format.getVertexSize();
			int at = format.getOffset(VertexFormatElement.POSITION);
			int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
			int normal = format.contains(VertexFormatElement.NORMAL) ? format.getOffset(VertexFormatElement.NORMAL) : -1;
			// in runs that fit the 64 KB MemoryStack, whole quads or triangles each: a big block item's model alone can overflow it
			int run = Math.max(12, CHUNK / stride / 12 * 12);
			if (shift && target instanceof ReserveBuffer reserve && reserve.zakoopt$accepts(format)) {
				// straight into the buffer's own memory: one copy instead of a stack copy and Sodium's push copying it again. The
				// buffer may be mapped GPU memory (the immediate ring), slow to read back: the shift reads the mesh, it only writes there
				int size = segment.count * stride;
				long src = MemoryUtil.memAddress(segment.vertices);
				long dst = reserve.zakoopt$address(reserve.zakoopt$reserve(segment.count));
				MemoryUtil.memCopy(src, dst, size);
				for (long o = at; o < size; o += stride) {
					MemoryUtil.memPutFloat(dst + o, MemoryUtil.memGetFloat(src + o) + tx);
					MemoryUtil.memPutFloat(dst + o + 4, MemoryUtil.memGetFloat(src + o + 4) + ty);
					MemoryUtil.memPutFloat(dst + o + 8, MemoryUtil.memGetFloat(src + o + 8) + tz);
					if (uv2 >= 0 && MemoryUtil.memGetInt(src + o - at + uv2) == LIGHT_MARK) {
						MemoryUtil.memPutInt(dst + o - at + uv2, light);
					}
				}
				continue;
			}
			if (shift) {
				for (int first = 0; first < segment.count; first += run) {
					int n = Math.min(run, segment.count - first);
					try (MemoryStack stack = MemoryStack.stackPush()) {
						long dst = RawVertices.scratch(n * stride);
						MemoryUtil.memCopy(MemoryUtil.memAddress(segment.vertices) + (long) first * stride, dst, (long) n * stride);
						for (long v = dst + at, end = dst + (long) n * stride; v < end; v += stride) {
							MemoryUtil.memPutFloat(v, MemoryUtil.memGetFloat(v) + tx);
							MemoryUtil.memPutFloat(v + 4, MemoryUtil.memGetFloat(v + 4) + ty);
							MemoryUtil.memPutFloat(v + 8, MemoryUtil.memGetFloat(v + 8) + tz);
							if (uv2 >= 0 && MemoryUtil.memGetInt(v - at + uv2) == LIGHT_MARK) {
								MemoryUtil.memPutInt(v - at + uv2, light);
							}
						}
						RawVertices.push(target, stack, dst, n, format);
					}
				}
				continue;
			}
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
			for (int first = 0; first < segment.count; first += run) {
				int n = Math.min(run, segment.count - first);
				try (MemoryStack stack = MemoryStack.stackPush()) {
					long dst = RawVertices.scratch(n * stride);
					MemoryUtil.memCopy(MemoryUtil.memAddress(segment.vertices) + (long) first * stride, dst, (long) n * stride);
					int i = first;
					for (long v = dst, end = dst + (long) n * stride; v < end; v += stride, i++) {
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
					RawVertices.push(target, stack, dst, n, format);
				}
			}
		}
		return true;
	}

	private Segment index(RenderType type, VertexFormat format, ByteBuffer vertices, int count) {
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
		return new Segment(type, format, vertices, count, positions, positionIndex, normalList.stream().mapToInt(Integer::intValue).toArray(), normalIndex);
	}

	private byte normal(float c) {
		return (byte) (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F);
	}

	// one segment per run of draws into the same render type, as the vanilla buffer source would see them
	public void free(List<Segment> segments) {
		for (Segment segment : segments) {
			MemoryUtil.memFree(segment.vertices);
		}
	}

	@RequiredArgsConstructor
	private static final class Recorder implements MultiBufferSource {
		final List<Segment> segments = new ArrayList<>();
		final boolean owned;
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
					ByteBuffer copy = owned ? MemoryUtil.memAlloc(src.remaining()) : ByteBuffer.allocateDirect(src.remaining());
					MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(copy), src.remaining());
					segments.add(index(type, mesh.drawState().format(), copy, mesh.drawState().vertexCount()));
				}
			}
			builder = null;
			type = null;
		}
	}
}
