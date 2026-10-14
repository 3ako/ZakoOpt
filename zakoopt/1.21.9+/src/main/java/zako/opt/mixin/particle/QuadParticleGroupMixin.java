package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.feature.Features;
import zako.opt.particle.ParallelParticles;
import zako.opt.particle.ParticleLod;

@Mixin(QuadParticleGroup.class)
public abstract class QuadParticleGroupMixin extends ParticleGroup<SingleQuadParticle> {
	@Shadow
	@Final
	QuadParticleRenderState particleTypeRenderState;

	private QuadParticleGroupMixin(ParticleEngine engine) {
		super(engine);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(Frustum frustum, Camera camera, float partialTick, CallbackInfoReturnable<ParticleGroupRenderState> cir) {
		if (ParallelParticles.extract(this.particles, particleTypeRenderState, frustum, camera, partialTick)) {
			cir.setReturnValue(particleTypeRenderState);
		}
	}

	// on the vanilla path, per particle per frame: a plain boolean in and out, so no argument array or boxed doubles. The
	// position is the particle's own, as ParallelParticles reads it for the same frustum test
	@ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
	private boolean zakoopt$lod(boolean visible, @Local(argsOnly = true) Camera camera, @Local SingleQuadParticle particle) {
		if (!visible || !Features.PARTICLE_LOD.on) {
			return visible;
		}
		ParticleAccessor pos = (ParticleAccessor) particle;
		return ParticleLod.keep(particle, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), camera.position());
	}
}
