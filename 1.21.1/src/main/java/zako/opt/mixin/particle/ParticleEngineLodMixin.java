package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
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
// NeoForge moves the particle loop into a render overload that also takes a frustum and a type filter; both are named
public class ParticleEngineLodMixin {
	@Shadow
	protected ClientLevel level;

	@Inject(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At("HEAD"))
	private void zakoopt$tick(CallbackInfo ci) {
		ParticleLod.tick = level.getGameTime();
	}

	// a condition, not a wrap: Operation.call boxed the partial tick into an array for every particle of every frame
	@WrapWithCondition(method = {"render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private boolean zakoopt$lod(Particle particle, VertexConsumer consumer, Camera camera, float partialTick) {
		if (particle instanceof SingleQuadParticle quad) {
			ParticleAccessor pos = (ParticleAccessor) particle;
			return ParticleLod.keep(quad, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), camera.getPosition());
		}
		return true;
	}
}
