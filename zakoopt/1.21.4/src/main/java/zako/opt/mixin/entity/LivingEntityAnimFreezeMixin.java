package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.AnimFreeze;
import zako.opt.entity.StateEntity;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityAnimFreezeMixin {
	// the consumer may be the skin atlas' sprite wrapper; replayed vertices go through it, so Sodium's push remaps their UVs
	@WrapOperation(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void zakoopt$frozenAnimation(EntityModel<?> model, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, Operation<Void> original,
										 LivingEntityRenderState state, @Local RenderType type) {
		if (ZakoOptConfig.entityAnimFreeze()) {
			Entity entity = ((StateEntity) state).zakoopt$entity();
			if (entity != null && AnimFreeze.render(entity, model, type, null, poseStack.last().pose(), consumer,
					target -> original.call(model, poseStack, target, light, overlay, color))) {
				return;
			}
		}
		original.call(model, poseStack, consumer, light, overlay, color);
	}
}
