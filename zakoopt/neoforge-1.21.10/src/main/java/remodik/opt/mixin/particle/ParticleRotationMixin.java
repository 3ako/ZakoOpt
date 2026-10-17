package remodik.opt.mixin.particle;

import remodik.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.SingleQuadParticle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// every quad particle made a quaternion per frame to face the camera, and the render state stores its four floats at once
@Mixin(SingleQuadParticle.class)
public class ParticleRotationMixin {
	@Unique
	private static final Quaternionf ZAKOOPT$FACING = new Quaternionf();

	@WrapOperation(method = "extract", at = @At(value = "NEW", target = "()Lorg/joml/Quaternionf;"))
	private Quaternionf zakoopt$facing(Operation<Quaternionf> original) {
		// workers of the parallel particle pass make their own
		return Features.FEWER_ALLOCATIONS.on && RenderSystem.isOnRenderThread() ? ZAKOOPT$FACING.identity() : original.call();
	}
}
