package zako.opt.particle;

import zako.opt.feature.Features;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;
import zako.opt.mixin.particle.ParticleAccessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

// 1.21.8 has no extraction phase: particles write vertices while rendering. Each block of particles is written
// into its own buffer on a worker, then the blocks are appended to the real buffer in order.
@UtilityClass
public class ParallelQuadParticles {
	private final int MIN_PARTICLES = 1024;
	private final int BLOCK = 256;
	private final Class<?>[] RENDER_ARGS = {VertexConsumer.class, Camera.class, float.class};
	private final List<ByteBufferBuilder> LOCAL = new ArrayList<>();
	private final List<BufferBuilder> BUILDERS = new ArrayList<>();
	private Particle[] scratch = new Particle[0];
	// a mod's render may call RenderSystem (Explosion Overhaul's PlasmaParticle does); matched by signature, the name is remapped per loader
	private final ClassValue<Boolean> SAFE = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			if (!SingleQuadParticle.class.isAssignableFrom(type)) {
				return false;
			}
			for (Class<?> c = type; c != SingleQuadParticle.class; c = c.getSuperclass()) {
				if (c.getName().startsWith("net.minecraft.")) {
					continue;
				}
				for (Method m : c.getDeclaredMethods()) {
					if (Arrays.equals(m.getParameterTypes(), RENDER_ARGS)) {
						return false;
					}
				}
			}
			return true;
		}
	};

	// false = let the vanilla single-threaded loop run
	public boolean render(Queue<Particle> particles, VertexConsumer target, VertexFormat format, Camera camera, float partialTick) {
		int n = particles.size();
		if (n < MIN_PARTICLES || !Features.PARALLEL_PARTICLES.on) {
			return false;
		}
		if (!RawVertices.accepts(target, format)) {
			return false;
		}
		Particle[] arr = particles.toArray(scratch);
		scratch = arr;
		try {
			for (int i = 0; i < n; i++) {
				// other particle kinds may draw anything; only plain quads are known to be safe off-thread
				if (!SAFE.get(arr[i].getClass())) {
					return false;
				}
			}
			int blocks = (n + BLOCK - 1) / BLOCK;
			while (LOCAL.size() < blocks) {
				LOCAL.add(new ByteBufferBuilder(BLOCK * 4 * format.getVertexSize()));
			}
			BUILDERS.clear();
			for (int b = 0; b < blocks; b++) {
				BUILDERS.add(new BufferBuilder(LOCAL.get(b), VertexFormat.Mode.QUADS, format));
			}
			Vec3 cam = camera.getPosition();
			Throwable error = Workers.run(blocks, b -> renderRange(arr, b * BLOCK, Math.min(n, (b + 1) * BLOCK), BUILDERS.get(b), camera, cam, partialTick));
			try (MemoryStack stack = MemoryStack.stackPush()) {
				for (BufferBuilder builder : BUILDERS) {
					// build() also has to run after a failure: it releases the block's memory for the next frame
					try (MeshData mesh = builder.build()) {
						if (mesh != null && error == null) {
							RawVertices.push(target, stack, MemoryUtil.memAddress(mesh.vertexBuffer()), mesh.drawState().vertexCount(), format);
						}
					}
				}
			}
			if (error != null) {
				throw new ReportedException(CrashReport.forThrowable(error, "Rendering Particle (zakoopt parallel)"));
			}
			return true;
		} finally {
			// arr keeps last frame's particles alive until the next toArray overwrites them: cheaper than clearing it every frame
			BUILDERS.clear();
		}
	}

	private void renderRange(Particle[] arr, int from, int to, BufferBuilder out, Camera camera, Vec3 cam, float partialTick) {
		for (int i = from; i < to; i++) {
			SingleQuadParticle p = (SingleQuadParticle) arr[i];
			ParticleAccessor pos = (ParticleAccessor) p;
			if (ParticleLod.keep(p, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), cam)) {
				p.render(out, camera, partialTick);
			}
		}
	}
}
