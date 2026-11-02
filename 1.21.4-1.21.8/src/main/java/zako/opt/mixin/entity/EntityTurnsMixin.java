package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.Turns;

// every rotation in setupRotations goes straight into poseStack.mulPose
@Mixin(LivingEntityRenderer.class)
public class EntityTurnsMixin {
	@WrapOperation(method = "setupRotations", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"))
	private Quaternionf zakoopt$turn(Axis axis, float degrees, Operation<Quaternionf> original) {
		Quaternionf turn = Turns.degrees(axis, degrees);
		return turn != null ? turn : original.call(axis, degrees);
	}
}
