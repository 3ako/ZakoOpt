package remodik.opt.block;

import remodik.opt.feature.Features;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import remodik.opt.gl.RawVertices;

// piston-moved blocks: tesselate each BlockState once per tint, then per frame only translate positions and set one light value
@UtilityClass
public class MovingBlockCache {
	private final Map<Key, Entry> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, MovingBlockCache.Entry> eldest) {
			if (size() <= 1024) {
				return false;
			}
			free(eldest.getValue());
			return true;
		}
	};

	// grass, leaves, vines... take their colour from the biome where they are tesselated: the tint is part of the key, or every
	// moved block of a state would show the colour of the first one
	@AllArgsConstructor
	@EqualsAndHashCode
	private static final class Key {
		BlockState state;
		int tint;
	}

	private record Layer(RenderType type, ByteBuffer vertices, int count) {
	}

	private record Entry(BlockStateModel model, Layer[] layers) {
	}

	// render thread only: the key a moving block is looked up with, copied only when a new entry is stored
	private final Key LOOKUP = new Key(null, 0);
	private final Vector3f TMP = new Vector3f();

	// renders what it can and removes it from the list; the rest goes the usual Fabric/vanilla way
	public void render(List<SubmitNodeStorage.MovingBlockSubmit> submits, MultiBufferSource.BufferSource buffers, BlockRenderDispatcher dispatcher) {
		if (!Features.MOVING_BLOCK_CACHE.on || submits.isEmpty()) {
			return;
		}
		submits.removeIf(submit -> {
			MovingBlockRenderState state = submit.movingBlockRenderState();
			BlockStateModel model = dispatcher.getBlockModel(state.blockState);
			if (!(model instanceof SingleVariant)) {
				return false;
			}
			LOOKUP.state = state.blockState;
			LOOKUP.tint = Minecraft.getInstance().getBlockColors().getColor(state.blockState, state, state.blockPos, 0);
			Entry entry = CACHE.get(LOOKUP);
			if (entry == null || entry.model != model) {
				if (entry != null) {
					free(entry);
				}
				entry = capture(state, model, dispatcher);
				CACHE.put(new Key(LOOKUP.state, LOOKUP.tint), entry);
			}
			Layer[] layers = entry.layers;
			for (Layer layer : layers) {
				if (!RawVertices.accepts(buffers.getBuffer(layer.type), layer.type.format())) {
					return false;
				}
			}
			int light = LevelRenderer.getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, state, state.blockState, state.blockPos);
			for (Layer layer : layers) {
				emit(layer, submit.pose(), light, buffers.getBuffer(layer.type));
			}
			return true;
		});
	}

	private void free(Entry entry) {
		for (Layer layer : entry.layers) {
			MemoryUtil.memFree(layer.vertices);
		}
	}

	private Entry capture(MovingBlockRenderState state, BlockStateModel model, BlockRenderDispatcher dispatcher) {
		Map<RenderType, ByteBufferBuilder> bytes = new LinkedHashMap<>();
		Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
		MultiBufferSource source = type -> builders.computeIfAbsent(type, t -> {
			ByteBufferBuilder b = new ByteBufferBuilder(1024);
			bytes.put(t, b);
			return new BufferBuilder(b, t.mode(), t.format());
		});
		// as NeoForge's BlockFeatureRenderer draws moving blocks: each part in the moving variant of its own layer
		List<BlockModelPart> parts = model.collectParts(state, state.blockPos, state.blockState, RandomSource.create(state.blockState.getSeed(state.randomSeedPos)));
		dispatcher.getModelRenderer().tesselateBlock(state, parts, state.blockState, state.blockPos, new PoseStack(),
				layer -> source.getBuffer(RenderTypeHelper.getMovingBlockRenderType(layer)), false, OverlayTexture.NO_OVERLAY);
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
		return new Entry(model, layers.toArray(new Layer[0]));
	}

	private void emit(Layer layer, Matrix4f pose, int light, VertexConsumer target) {
		VertexFormat format = layer.type.format();
		int stride = format.getVertexSize();
		int pos = format.getOffset(VertexFormatElement.POSITION);
		int uv2 = format.contains(VertexFormatElement.UV2) ? format.getOffset(VertexFormatElement.UV2) : -1;
		int size = layer.count * stride;
		Vector3f tmp = TMP;
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
