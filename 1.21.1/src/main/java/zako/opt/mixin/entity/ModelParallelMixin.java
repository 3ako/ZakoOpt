package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.Model;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.ParallelModels;

@Mixin(HierarchicalModel.class)
// 1.21.1: Model.renderToBuffer is abstract; only models with a single root (HierarchicalModel) go parallel
public class ModelParallelMixin {
	@Inject(method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, CallbackInfo ci) {
		if (ParallelModels.add((Model) (Object) this, poseStack, consumer, light, overlay, color)) {
			ci.cancel();
		}
	}
}
