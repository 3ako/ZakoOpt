package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

@Mixin(ItemClusterRenderState.class)
public class ItemClusterRenderStateMixin {
	@ModifyExpressionValue(method = "extractItemGroupRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;getRenderedAmount(I)I"))
	private int zakoopt$singleCopy(int copies) {
		return ZakoOptConfig.itemSingleCopy() ? 1 : copies;
	}
}
