package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z"))
	private boolean zakoopt$lod(Frustum frustum, double x, double y, double z, Operation<Boolean> original,
								Frustum frustumArg, Camera camera, float partialTick, @Local SingleQuadParticle particle) {
		return original.call(frustum, x, y, z) && ParticleLod.keep(particle, x, y, z, camera.position());
	}
}
