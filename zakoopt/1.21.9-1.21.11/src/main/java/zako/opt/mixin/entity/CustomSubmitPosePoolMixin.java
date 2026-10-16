package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.PosePool;

// before 26.1 custom geometry copies its pose here rather than in SubmitNodeCollection
@Mixin(CustomFeatureRenderer.Storage.class)
public class CustomSubmitPosePoolMixin {
	@WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;copy()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"))
	private PoseStack.Pose zakoopt$pooledPose(PoseStack.Pose pose, Operation<PoseStack.Pose> original) {
		return PosePool.pooling() ? PosePool.copy(pose) : original.call(pose);
	}
}
