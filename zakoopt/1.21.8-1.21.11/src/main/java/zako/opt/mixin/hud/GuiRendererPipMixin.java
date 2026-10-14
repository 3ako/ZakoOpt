package zako.opt.mixin.hud;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.HudCache;

@Mixin(GuiRenderer.class)
public class GuiRendererPipMixin {
	@Inject(method = "preparePictureInPicture", at = @At("HEAD"))
	private void zakoopt$realTarget(CallbackInfo ci) {
		HudCache.pipPrepare = true;
	}

	@Inject(method = "preparePictureInPicture", at = @At("RETURN"))
	private void zakoopt$hudTarget(CallbackInfo ci) {
		HudCache.pipPrepare = false;
	}
}
