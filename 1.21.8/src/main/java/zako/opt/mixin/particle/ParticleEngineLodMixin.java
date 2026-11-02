package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.particle.ParticleLod;

@Mixin(ParticleEngine.class)
public class ParticleEngineLodMixin {
	// a condition, not a wrap: Operation.call boxed the partial tick into an array for every particle of every frame
	@WrapWithCondition(method = "renderParticleType", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private static boolean zakoopt$lod(Particle particle, VertexConsumer consumer, Camera camera, float partialTick) {
		if (particle instanceof SingleQuadParticle quad) {
			ParticleAccessor pos = (ParticleAccessor) particle;
			return ParticleLod.keep(quad, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), camera.getPosition());
		}
		return true;
	}
}
