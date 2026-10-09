package zako.opt.mixin.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.particle.ParallelQuadParticles;

import java.util.Queue;

@Mixin(ParticleEngine.class)
public class ParticleEngineParallelMixin {
	@Inject(method = "renderParticleType", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$parallel(Camera camera, float partialTick, MultiBufferSource.BufferSource buffers, ParticleRenderType particleType, Queue<Particle> particles, CallbackInfo ci) {
		RenderType type = particleType.renderType();
		if (type != null && ParallelQuadParticles.render(particles, buffers.getBuffer(type), type.format(), camera, partialTick)) {
			ci.cancel();
		}
	}
}
