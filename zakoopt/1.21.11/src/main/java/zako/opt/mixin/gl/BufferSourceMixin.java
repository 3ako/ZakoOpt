package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiBufferSource.BufferSource.class)
public class BufferSourceMixin {
	@ModifyExpressionValue(method = "endBatch(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;sortOnUpload()Z"))
	private boolean zakoopt$skipEntitySort(boolean sort, RenderType renderType) {
		if (!sort) {
			return false;
		}
		String name = ((RenderTypeAccessor) renderType).zakoopt$name();
		return !(Features.NO_ENTITY_SORT.on && "entity_translucent".equals(name) || Features.NO_ITEM_SORT.on && "item_entity_translucent_cull".equals(name));
	}
}
