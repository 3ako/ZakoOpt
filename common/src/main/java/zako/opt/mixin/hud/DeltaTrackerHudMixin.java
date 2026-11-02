package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.hud.HudCache;
import zako.opt.hud.HudDelta;

@Mixin(DeltaTracker.Timer.class)
public class DeltaTrackerHudMixin {
	// while the cached HUD is drawn, a frame's delta is the time since its previous refresh
	@ModifyReturnValue(method = "getGameTimeDeltaTicks", at = @At("RETURN"))
	private float zakoopt$hudDelta(float delta) {
		return HudCache.suppressLiveParts ? HudDelta.sinceRefresh() : delta;
	}
}
