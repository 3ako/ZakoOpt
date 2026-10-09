package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import zako.opt.entity.ItemLod;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@Unique
	private static boolean zakoopt$flat;

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;[IIILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
	private static void zakoopt$lod(BakedModel model, int[] tints, int light, int overlay, PoseStack poseStack, VertexConsumer buffer, Operation<Void> original,
									@Local(argsOnly = true) ItemDisplayContext context) {
		if (context.firstPerson() || context == ItemDisplayContext.GUI || !ItemLod.enabled()) {
			original.call(model, tints, light, overlay, poseStack, buffer);
			return;
		}
		Matrix4f m = poseStack.last().pose();
		zakoopt$flat = m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32() > ItemLod.distanceSq();
		try {
			original.call(model, tints, light, overlay, poseStack, buffer);
		} finally {
			zakoopt$flat = false;
		}
	}

	@ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true)
	private static List<BakedQuad> zakoopt$flatten(List<BakedQuad> quads) {
		return zakoopt$flat ? ItemLod.flat(quads) : quads;
	}
}
