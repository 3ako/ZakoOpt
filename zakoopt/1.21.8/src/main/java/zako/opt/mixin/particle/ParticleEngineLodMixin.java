package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.feature.Features;
import zako.opt.particle.ParticleLod;

@Mixin(ParticleEngine.class)
public class ParticleEngineLodMixin {
	// per particle per frame: the arguments come in unboxed and the call is left to vanilla, no argument array
	@WrapWithCondition(method = "renderParticleType", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
	private static boolean zakoopt$lod(Particle particle, VertexConsumer consumer, Camera camera, float partialTick) {
		if (!Features.PARTICLE_LOD.on || !(particle instanceof SingleQuadParticle quad)) {
			return true;
		}
		ParticleAccessor pos = (ParticleAccessor) particle;
		return ParticleLod.keep(quad, pos.zakoopt$x(), pos.zakoopt$y(), pos.zakoopt$z(), camera.getPosition());
	}
}
