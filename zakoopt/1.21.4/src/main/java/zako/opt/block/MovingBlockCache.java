package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

// a block pushed by a piston is tesselated once (through tesselateBlock, so Fabric models work) and replayed every frame:
// positions through the pose, one light value for the whole block. The smooth lighting of every frame went, and with it
// the neighbour shape checks that merged the moving piston's collision shapes
@UtilityClass
public class MovingBlockCache {
	private final ByteBufferBuilder SCRATCH = new ByteBufferBuilder(4096);
	private final Map<Key, Mesh> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Mesh> eldest) {
			return size() > 1024;
		}
	};

	private record Key(BlockState state, long seed, boolean checkSides, int overlay) {
	}

	private record Mesh(BakedModel model, RenderType type, ByteBuffer vertices, int count) {
	}

	// false: draw this block the vanilla way
	public boolean render(BlockRenderDispatcher dispatcher, BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource buffers,
						  Level level, boolean checkSides, int overlay) {
		BakedModel model = dispatcher.getBlockModel(state);
		Key key = new Key(state, state.getSeed(pos), checkSides, overlay);
		Mesh entry = CACHE.get(key);
		if (entry == null || entry.model != model) {
			entry = capture(dispatcher, model, pos, state, level, key);
			CACHE.put(key, entry);
		}
		VertexConsumer target = buffers.getBuffer(entry.type);
		if (entry.count == 0) {
			return true;
		}
		if (!RawVertices.accepts(target, entry.type.format())) {
			return false;
		}
		emit(entry, poseStack.last().pose(), LevelRenderer.getLightColor(level, pos), target);
		return true;
	}

	private Mesh capture(BlockRenderDispatcher dispatcher, BakedModel model, BlockPos pos, BlockState state, Level level, Key key) {
		RenderType type = ItemBlockRenderTypes.getMovingBlockRenderType(state);
		BufferBuilder builder = new BufferBuilder(SCRATCH, type.mode(), type.format());
		dispatcher.getModelRenderer().tesselateBlock(level, model, state, pos, new PoseStack(), builder, key.checkSides, RandomSource.create(), key.seed, key.overlay);
		try (MeshData mesh = builder.build()) {
			if (mesh == null) {
				return new Mesh(model, type, null, 0);
			}
			ByteBuffer src = mesh.vertexBuffer();
			ByteBuffer copy = ByteBuffer.allocateDirect(src.remaining());
			MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(copy), src.remaining());
			return new Mesh(model, type, copy, mesh.drawState().vertexCount());
		}
	}

	private void emit(Mesh entry, Matrix4f pose, int light, VertexConsumer target) {
		VertexFormat format = entry.type.format();
		int stride = format.getVertexSize();
		int pos = format.getOffset(VertexFormatElement.POSITION);
		int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
		int size = entry.count * stride;
		Vector3f tmp = new Vector3f();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			long dst = stack.nmalloc(16, size);
			MemoryUtil.memCopy(MemoryUtil.memAddress(entry.vertices), dst, size);
			for (long v = dst, end = dst + size; v < end; v += stride) {
				pose.transformPosition(MemoryUtil.memGetFloat(v + pos), MemoryUtil.memGetFloat(v + pos + 4), MemoryUtil.memGetFloat(v + pos + 8), tmp);
				MemoryUtil.memPutFloat(v + pos, tmp.x);
				MemoryUtil.memPutFloat(v + pos + 4, tmp.y);
				MemoryUtil.memPutFloat(v + pos + 8, tmp.z);
				if (uv2 >= 0) {
					MemoryUtil.memPutInt(v + uv2, light);
				}
			}
			RawVertices.push(target, stack, dst, entry.count, format);
		}
	}
}
