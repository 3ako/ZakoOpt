package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

@Mixin(MultiBufferSource.BufferSource.class)
public class BufferSourceMixin {
	@ModifyExpressionValue(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;sortOnUpload()Z"))
	private boolean zakoopt$skipEntitySort(boolean sort, RenderType renderType) {
		if (!sort) {
			return false;
		}
		String name = ((RenderTypeAccessor) renderType).zakoopt$name();
		return !(ZakoOptConfig.noEntitySort() && "entity_translucent".equals(name) || ZakoOptConfig.noItemSort() && "item_entity_translucent_cull".equals(name));
	}
}
