package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.block.ItemMeshes;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = BeltRenderer.class, remap = false)
public class CreateBeltMixin {
	@WrapOperation(require = 0, method = "renderItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
	private void zakoopt$cachedItem(ItemRenderer renderer, ItemStack stack, ItemDisplayContext context, boolean left, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
									BakedModel model, Operation<Void> original) {
		if (!Features.CREATE_ITEMS.on || !ItemMeshes.render(BeltRenderer.class, stack, model, ms, buffer, light, overlay,
				recorder -> original.call(renderer, stack, context, left, new PoseStack(), recorder, light, overlay, model))) {
			original.call(renderer, stack, context, left, ms, buffer, light, overlay, model);
		}
	}
}
