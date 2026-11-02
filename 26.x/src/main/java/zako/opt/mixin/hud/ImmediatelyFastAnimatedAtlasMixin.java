package zako.opt.mixin.hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.GuiItemRefresh;

// ImmediatelyFast keeps animated items in an atlas it wipes at every frame end, so they must be redrawn every frame;
// on frames where the throttle keeps them, the wipe is skipped too and only the vanilla bookkeeping runs
@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.batch_animated_item_updates.AnimatedItemAtlas")
public class ImmediatelyFastAnimatedAtlasMixin {
	@Inject(method = "endFrame", at = @At("HEAD"), cancellable = true, require = 0)
	private void zakoopt$keepBetweenRefreshes(CallbackInfo ci) {
		if (!GuiItemRefresh.refresh()) {
			((GuiItemAtlasAccessor) this).zakoopt$allocator().endFrame();
			ci.cancel();
		}
	}
}
