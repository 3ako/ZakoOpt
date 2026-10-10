package zako.opt.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.RawVertices;

// piston-moved blocks: tesselate each BlockState once, then per frame only translate positions and set one light value
@UtilityClass
public class MovingBlockCache {
	private final Map<BlockState, Entry> CACHE = new IdentityHashMap<>();

	private record Layer(RenderType type, ByteBuffer vertices, int count) {
	}

	private record Entry(BlockStateModel model, List<Layer> layers) {
	}

	// renders what it can and removes it from the list; the rest goes the usual Fabric/vanilla way
	public void render(List<SubmitNodeStorage.MovingBlockSubmit> submits, MultiBufferSource.BufferSource buffers, BlockRenderDispatcher dispatcher) {
		if (!ZakoOptConfig.movingBlockCache() || submits.isEmpty()) {
			return;
		}
		submits.removeIf(submit -> {
			MovingBlockRenderState state = submit.movingBlockRenderState();
			BlockStateModel model = dispatcher.getBlockModel(state.blockState);
			if (!(model instanceof SingleVariant)) {
				return false;
			}
			Entry entry = CACHE.get(state.blockState);
			if (entry == null || entry.model != model) {
				entry = capture(state, model, dispatcher);
				CACHE.put(state.blockState, entry);
			}
			int light = LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, state, state.blockState, state.blockPos);
			for (Layer layer : entry.layers) {
				emit(layer, submit.pose(), light, buffers.getBuffer(layer.type));
			}
			return true;
		});
	}

	private Entry capture(MovingBlockRenderState state, BlockStateModel model, BlockRenderDispatcher dispatcher) {
		Map<RenderType, ByteBufferBuilder> bytes = new LinkedHashMap<>();
		Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
		MultiBufferSource source = type -> builders.computeIfAbsent(type, t -> {
			ByteBufferBuilder b = new ByteBufferBuilder(1024);
			bytes.put(t, b);
			return new BufferBuilder(b, t.mode(), t.format());
		});
		dispatcher.getModelRenderer().render(state, model, state.blockState, state.blockPos, new PoseStack(),
				RenderLayerHelper.movingDelegate(source), false, state.blockState.getSeed(state.randomSeedPos), OverlayTexture.NO_OVERLAY);
		List<Layer> layers = new ArrayList<>();
		builders.forEach((type, builder) -> {
			try (MeshData mesh = builder.build()) {
				if (mesh != null) {
					ByteBuffer src = mesh.vertexBuffer();
					ByteBuffer copy = MemoryUtil.memAlloc(src.remaining());
					MemoryUtil.memCopy(src, copy);
					layers.add(new Layer(type, copy, mesh.drawState().vertexCount()));
				}
			}
		});
		bytes.values().forEach(ByteBufferBuilder::close);
		return new Entry(model, layers);
	}

	private void emit(Layer layer, Matrix4f pose, int light, VertexConsumer target) {
		VertexFormat format = layer.type.format();
		int stride = format.getVertexSize();
		int pos = format.getOffset(VertexFormatElement.POSITION);
		int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
		int size = layer.count * stride;
		Vector3f tmp = new Vector3f();
		try (MemoryStack stack = MemoryStack.stackPush()) {
			long dst = stack.nmalloc(16, size);
			MemoryUtil.memCopy(MemoryUtil.memAddress(layer.vertices), dst, size);
			for (long v = dst, end = dst + size; v < end; v += stride) {
				pose.transformPosition(MemoryUtil.memGetFloat(v + pos), MemoryUtil.memGetFloat(v + pos + 4), MemoryUtil.memGetFloat(v + pos + 8), tmp);
				MemoryUtil.memPutFloat(v + pos, tmp.x);
				MemoryUtil.memPutFloat(v + pos + 4, tmp.y);
				MemoryUtil.memPutFloat(v + pos + 8, tmp.z);
				if (uv2 >= 0) {
					MemoryUtil.memPutInt(v + uv2, light);
				}
			}
			RawVertices.push(target, stack, dst, layer.count, format);
		}
	}
}
