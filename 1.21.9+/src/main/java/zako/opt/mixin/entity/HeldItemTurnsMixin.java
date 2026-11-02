package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.Turns;

// both rotations of a held item go straight into poseStack.mulPose
// 26.3 rotates through PoseStack.rotateDegrees, which makes no quaternion here: nothing to wrap there
@Mixin(ItemInHandLayer.class)
public class HeldItemTurnsMixin {
	@WrapOperation(method = "submitArmWithItem", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"))
	private Quaternionf zakoopt$turn(Axis axis, float degrees, Operation<Quaternionf> original) {
		Quaternionf turn = Turns.degrees(axis, degrees);
		return turn != null ? turn : original.call(axis, degrees);
	}
}
