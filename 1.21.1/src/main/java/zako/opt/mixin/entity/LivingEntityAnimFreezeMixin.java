package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.AnimFreeze;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityAnimFreezeMixin {
	// the render type the body is drawn with this call; a @Local of it does not resolve once the method is rewritten
	@Unique
	private RenderType zakoopt$type;

	@ModifyExpressionValue(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;"))
	private RenderType zakoopt$renderType(RenderType type) {
		zakoopt$type = type;
		return type;
	}

	// a condition, not a wrap: Operation.call boxed the three ints into an array for every model of every frame
	// the consumer may be the skin atlas' sprite wrapper; replayed vertices go through it, so Sodium's push remaps their UVs
	@WrapWithCondition(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private boolean zakoopt$frozenAnimation(EntityModel<?> model, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color,
											LivingEntity entity) {
		RenderType type = zakoopt$type;
		if (Features.ENTITY_ANIM_FREEZE.on && type != null) {
			return !AnimFreeze.render(entity, model, type, null, poseStack.last().pose(), consumer,
					target -> model.renderToBuffer(poseStack, target, light, overlay, color));
		}
		return true;
	}
}
