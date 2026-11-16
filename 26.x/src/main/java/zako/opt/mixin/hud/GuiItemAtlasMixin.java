package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.hud.GuiItemRefresh;

@Mixin(GuiItemAtlas.class)
public class GuiItemAtlasMixin {
	// an animated item's slot is freed after every frame and redrawn; reported static in between, it is redrawn once per monitor refresh.
	// ImmediatelyFast's animated atlas (a subclass, used from 3 animated models up) wipes itself every frame: left alone
	@ModifyExpressionValue(method = "getOrUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;isAnimated()Z"))
	private boolean zakoopt$throttleAnimated(boolean animated) {
		return animated && (GuiItemRefresh.refresh() || (Object) this.getClass() != GuiItemAtlas.class);
	}
}
