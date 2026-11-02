package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.PosePool;

@Mixin(SubmitNodeCollection.class)
public class SubmitPosePoolMixin {
	@WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;copy()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"))
	private PoseStack.Pose zakoopt$pooledPose(PoseStack.Pose pose, Operation<PoseStack.Pose> original) {
		return PosePool.pooling() ? PosePool.copy(pose) : original.call(pose);
	}
}
