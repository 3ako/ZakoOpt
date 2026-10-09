package zako.opt.mixin.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.HudCache;

@Mixin(RenderTarget.class)
public class RenderTargetHudMixin {
	// every RenderType's MAIN_TARGET output shard rebinds the main target in its setup
	@Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
	private void zakoopt$hudTarget(boolean setViewport, CallbackInfo ci) {
		RenderTarget hud = HudCache.redirect((RenderTarget) (Object) this);
		if (hud != null) {
			hud.bindWrite(setViewport);
			ci.cancel();
		}
	}
}
