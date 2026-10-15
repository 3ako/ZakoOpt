package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.particle.ParticleLod;

@Mixin(ParticleEngine.class)
public class ParticleEngineLodMixin {
	@Shadow
	protected ClientLevel level;

	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$tick(CallbackInfo ci) {
		ParticleLod.tick = level.getGameTime();
	}

	@WrapOperation(method = "renderParticleType", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private static void zakoopt$lod(Particle particle, VertexConsumer consumer, Camera camera, float partialTick, Operation<Void> original) {
		if (particle instanceof SingleQuadParticle quad) {
			ParticleAccessor pos = (ParticleAccessor) particle;
			if (!ParticleLod.keep(quad, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), camera.getPosition())) {
				return;
			}
		}
		original.call(particle, consumer, camera, partialTick);
	}
}
