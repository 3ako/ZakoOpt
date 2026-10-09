package zako.opt.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.update.UpdateCheck;

@Mixin(TitleScreen.class)
public class TitleScreenUpdateMixin {
	@Inject(method = "init", at = @At("RETURN"))
	private void zakoopt$updateCheck(CallbackInfo ci) {
		UpdateCheck.start();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void zakoopt$offerUpdate(CallbackInfo ci) {
		UpdateCheck.offer((TitleScreen) (Object) this);
	}
}
