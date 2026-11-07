package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Quaternionf;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// the transform's rotation never changes, yet every item built a new quaternion from degrees every frame
@Mixin(ItemTransform.class)
public class ItemTransformRotationMixin {
	@Shadow
	@Final
	private Vector3fc rotation;
	@Shadow
	@Final
	private Vector3fc translation;
	@Shadow
	@Final
	private Vector3fc scale;
	@Unique
	private Quaternionf zakoopt$right;
	@Unique
	private Quaternionf zakoopt$left;

	@Inject(method = "apply", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedRotation(boolean leftHand, PoseStack.Pose pose, CallbackInfo ci) {
		if (!Features.FEWER_ALLOCATIONS.on || (Object) this == ItemTransform.NO_TRANSFORM) {
			return;
		}
		Quaternionf q = leftHand ? zakoopt$left : zakoopt$right;
		if (q == null) {
			float s = leftHand ? -1 : 1, rad = (float) (Math.PI / 180.0);
			q = new Quaternionf().rotationXYZ(rotation.x() * rad, s * rotation.y() * rad, s * rotation.z() * rad);
			if (leftHand) {
				zakoopt$left = q;
			} else {
				zakoopt$right = q;
			}
		}
		pose.translate(leftHand ? -translation.x() : translation.x(), translation.y(), translation.z());
		pose.rotate(q);
		pose.scale(scale.x(), scale.y(), scale.z());
		pose.translate(-0.5F, -0.5F, -0.5F);
		ci.cancel();
	}
}
