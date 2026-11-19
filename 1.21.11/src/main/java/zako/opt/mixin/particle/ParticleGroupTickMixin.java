package zako.opt.mixin.particle;

import zako.opt.particle.ParallelParticleTick;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ParticleGroup.class)
public abstract class ParticleGroupTickMixin {
	@Shadow
	@Final
	protected Queue particles;

	@Shadow
	@Final
	protected ParticleEngine engine;

	// item pickups and elder guardians tick against renderer state, so they stay single-threaded; quads are the whole
	// particle count anyway
	@Inject(method = "tickParticles", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(CallbackInfo ci) {
		if ((Object) this instanceof QuadParticleGroup && ParallelParticleTick.tick(this.particles, this.engine)) {
			ParallelParticleTick.removeDead(this.particles, this.engine);
			ci.cancel();
		}
	}
}
