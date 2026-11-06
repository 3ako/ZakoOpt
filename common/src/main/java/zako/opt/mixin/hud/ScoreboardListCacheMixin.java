package zako.opt.mixin.hud;

import zako.opt.feature.Features;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// the sidebar lists one objective by walking every score holder on the server, each time the HUD redraws it
@Mixin(Scoreboard.class)
public class ScoreboardListCacheMixin {
	@Unique
	private int zakoopt$version;
	@Unique
	private int zakoopt$cachedVersion = -1;
	@Unique
	private Objective zakoopt$cachedObjective;
	@Unique
	private List<PlayerScoreEntry> zakoopt$cached;

	// getOrCreatePlayerScore can add a zero score that is listed before anything is set
	@Inject(method = {"getOrCreatePlayerScore(Lnet/minecraft/world/scores/ScoreHolder;Lnet/minecraft/world/scores/Objective;Z)Lnet/minecraft/world/scores/ScoreAccess;",
			"resetAllPlayerScores", "resetSinglePlayerScore", "removeObjective", "onScoreChanged"}, at = @At("HEAD"))
	private void zakoopt$changed(CallbackInfo ci) {
		zakoopt$version++;
	}

	@Inject(method = "listPlayerScores(Lnet/minecraft/world/scores/Objective;)Ljava/util/Collection;", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedList(Objective objective, CallbackInfoReturnable<Collection<PlayerScoreEntry>> cir) {
		if (Features.MICRO_OPTS.on && objective == zakoopt$cachedObjective && zakoopt$version == zakoopt$cachedVersion) {
			cir.setReturnValue(new ArrayList<>(zakoopt$cached));
		}
	}

	@Inject(method = "listPlayerScores(Lnet/minecraft/world/scores/Objective;)Ljava/util/Collection;", at = @At("RETURN"))
	private void zakoopt$storeList(Objective objective, CallbackInfoReturnable<Collection<PlayerScoreEntry>> cir) {
		zakoopt$cachedObjective = objective;
		zakoopt$cachedVersion = zakoopt$version;
		zakoopt$cached = new ArrayList<>(cir.getReturnValue());
	}
}
