package remodik.opt.hud;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import remodik.opt.Mods;

import java.util.Objects;

// the HUD cache only saves work when several frames share one HUD refresh; near one frame per refresh it redraws the HUD
// anyway and just adds a full-screen clear and blend, which costs real FPS on integrated GPUs and 60 Hz monitors
@UtilityClass
@Slf4j(topic = "zakoopt")
public class HudWorth {
	private final double OFF_BELOW = 1.5;
	private final double ON_ABOVE = 2.0;
	private double frameNs;
	private long last;
	@Getter
	private boolean worth = true;
	@Getter
	private boolean mainSwapped;
	// the mod that swapped it, when the target's class gives it away; null: unknown
	@Getter
	private String swappedBy;

	public boolean update(long now, long refreshNs) {
		long dt = now - last;
		last = now;
		if (dt > 0 && dt < 1_000_000_000L) {
			frameNs = frameNs == 0 ? dt : frameNs * 0.95 + dt * 0.05;
		}
		double framesPerRefresh = frameNs == 0 ? ON_ABOVE : refreshNs / frameNs;
		if (worth ? framesPerRefresh < OFF_BELOW : framesPerRefresh > ON_ABOVE) {
			worth = !worth;
		}
		return worth;
	}

	// another mod draws the HUD into a window-sized target of its own instead of the game's main one: the HUD texture would stay
	// empty and the HUD would blink at the monitor rate, so the cache stays off for the session
	public void mainSwapped(Object target) {
		if (!mainSwapped) {
			mainSwapped = true;
			swappedBy = Mods.owner(target.getClass());
			if (swappedBy != null) {
				log.warn("HUD cache turned off: {} swaps the main render target while the HUD draws", swappedBy);
			} else {
				// the stack holds whatever draws the HUD (minimaps and the like), not necessarily the mod that swapped the target, so it only
				// goes to the log as a lead
				String suspect = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
						.walk(frames -> frames.map(Mods::owner).filter(Objects::nonNull).findFirst().orElse(null));
				log.warn("HUD cache turned off: another mod swaps the main render target while the HUD draws{}", suspect == null ? "" : " (possibly " + suspect + ")");
			}
		}
	}
}
