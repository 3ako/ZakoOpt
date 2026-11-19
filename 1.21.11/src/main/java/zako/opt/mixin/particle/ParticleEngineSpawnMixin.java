package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.particle.ParallelParticleTick;

@Mixin(ParticleEngine.class)
public class ParticleEngineSpawnMixin {
	// while the workers tick, spawned particles wait in a thread-safe queue instead of the engine's plain ArrayDeque
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void zakoopt$stage(Particle particle, CallbackInfo ci) {
		if (ParallelParticleTick.stages(particle)) {
			ci.cancel();
		}
	}

	// makeParticle reads the shared engine random; two worker threads spawning at once would trip vanilla's
	// threading detector, so the creation is serialized while the feature is on
	@WrapOperation(method = "makeParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleProvider;createParticle(Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/util/RandomSource;)Lnet/minecraft/client/particle/Particle;"))
	private Particle zakoopt$lockRandom(ParticleProvider provider, ParticleOptions options, ClientLevel level, double x, double y, double z, double xs, double ys, double zs,
										 RandomSource random, Operation<Particle> original) {
		if (!ParallelParticleTick.locking()) {
			return original.call(provider, options, level, x, y, z, xs, ys, zs, random);
		}
		synchronized (ParallelParticleTick.RANDOM_LOCK) {
			return original.call(provider, options, level, x, y, z, xs, ys, zs, random);
		}
	}
}
