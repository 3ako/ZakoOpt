package zako.opt.particle;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import lombok.experimental.UtilityClass;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zako.opt.ZakoOptConfig;
import zako.opt.mixin.particle.QuadParticleRenderStateAccessor;
import zako.opt.mixin.particle.QuadParticleStorageInvoker;

@UtilityClass
public class ParallelParticleVertices {
	private final int BLOCK = 256;
	private final VertexFormat FORMAT = DefaultVertexFormat.PARTICLE;
	private final int STRIDE = FORMAT.getVertexSize();
	private final int POS = FORMAT.getOffset(VertexFormatElement.POSITION);
	private final int UV = FORMAT.getOffset(VertexFormatElement.UV0);
	private final int COLOR = FORMAT.getOffset(VertexFormatElement.COLOR);
	private final int LIGHT = FORMAT.getOffset(VertexFormatElement.UV2);
	private final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

	// null = let vanilla prepare() run
	public QuadParticleRenderState.PreparedBuffers prepare(QuadParticleRenderState state, ParticleFeatureRenderer.ParticleBufferCache cache) {
		QuadParticleRenderStateAccessor acc = (QuadParticleRenderStateAccessor) state;
		int particles = acc.zakoopt$particleCount();
		if (particles < 1024 || !LITTLE_ENDIAN || !ZakoOptConfig.parallelParticleVertices()) {
			return null;
		}
		// same layer order and offsets as vanilla, so PreparedLayer entries match the buffer
		Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> layers = new HashMap<>();
		List<Block> blocks = new ArrayList<>();
		int vertex = 0;
		for (Map.Entry<SingleQuadParticle.Layer, Object> e : acc.zakoopt$particles().entrySet()) {
			QuadParticleStorageInvoker storage = (QuadParticleStorageInvoker) e.getValue();
			int count = storage.zakoopt$count();
			if (count > 0) {
				layers.put(e.getKey(), new QuadParticleRenderState.PreparedLayer(vertex, count * 6));
				for (int from = 0; from < count; from += BLOCK) {
					blocks.add(new Block(storage.zakoopt$floats(), storage.zakoopt$ints(), from, Math.min(count, from + BLOCK), vertex + from * 4));
				}
			}
			vertex += count * 4;
		}
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(vertex * STRIDE)) {
			long base = bytes.reserve(vertex * STRIDE);
			Throwable error = Workers.run(blocks.size(), b -> write(blocks.get(b), base));
			if (error != null) {
				throw new RuntimeException("zakoopt parallel particle vertices", error);
			}
			ByteBufferBuilder.Result result = bytes.build();
			if (result == null) {
				return null;
			}
			try (result) {
				cache.write(result.byteBuffer());
			}
			int indexCount = particles * 6;
			RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(indexCount);
			GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
					.writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
			return new QuadParticleRenderState.PreparedBuffers(indexCount, transforms, layers);
		}
	}

	private record Block(float[] floats, int[] ints, int from, int to, int firstVertex) {
	}

	private void write(Block block, long base) {
		float[] f = block.floats;
		int[] in = block.ints;
		long ptr = base + (long) block.firstVertex * STRIDE;
		for (int p = block.from; p < block.to; p++) {
			int o = p * 12;
			float x = f[o], y = f[o + 1], z = f[o + 2];
			float qx = f[o + 3], qy = f[o + 4], qz = f[o + 5], qw = f[o + 6];
			float size = f[o + 7], u0 = f[o + 8], u1 = f[o + 9], v0 = f[o + 10], v1 = f[o + 11];
			int color = ARGB.toABGR(in[p * 2]);
			int light = in[p * 2 + 1];
			// corner order and UVs match QuadParticleRenderState.renderRotatedQuad
			ptr = vertex(ptr, qx, qy, qz, qw, 1.0F, -1.0F, size, x, y, z, u1, v1, color, light);
			ptr = vertex(ptr, qx, qy, qz, qw, 1.0F, 1.0F, size, x, y, z, u1, v0, color, light);
			ptr = vertex(ptr, qx, qy, qz, qw, -1.0F, 1.0F, size, x, y, z, u0, v0, color, light);
			ptr = vertex(ptr, qx, qy, qz, qw, -1.0F, -1.0F, size, x, y, z, u0, v1, color, light);
		}
	}

	private long vertex(long ptr, float qx, float qy, float qz, float qw, float vx, float vy, float size,
							   float x, float y, float z, float u, float v, int color, int light) {
		// rotate (vx, vy, 0) by q: v' = v + w*t + cross(q, t), t = 2 * cross(q, v)
		float tx = 2.0F * (-qz * vy);
		float ty = 2.0F * (qz * vx);
		float tz = 2.0F * (qx * vy - qy * vx);
		float rx = vx + qw * tx + (qy * tz - qz * ty);
		float ry = vy + qw * ty + (qz * tx - qx * tz);
		float rz = qw * tz + (qx * ty - qy * tx);
		MemoryUtil.memPutFloat(ptr + POS, rx * size + x);
		MemoryUtil.memPutFloat(ptr + POS + 4, ry * size + y);
		MemoryUtil.memPutFloat(ptr + POS + 8, rz * size + z);
		MemoryUtil.memPutFloat(ptr + UV, u);
		MemoryUtil.memPutFloat(ptr + UV + 4, v);
		MemoryUtil.memPutInt(ptr + COLOR, color);
		MemoryUtil.memPutInt(ptr + LIGHT, light);
		return ptr + STRIDE;
	}
}
