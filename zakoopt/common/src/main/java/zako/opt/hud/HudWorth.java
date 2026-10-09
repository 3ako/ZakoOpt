package zako.opt.hud;

import lombok.Getter;
import lombok.experimental.UtilityClass;

// the HUD cache only saves work when several frames share one HUD refresh; near one frame per refresh it redraws the HUD
// anyway and just adds a full-screen clear and blend, which costs real FPS on integrated GPUs and 60 Hz monitors
@UtilityClass
public class HudWorth {
	private final double OFF_BELOW = 1.5;
	private final double ON_ABOVE = 2.0;
	private double frameNs;
	private long last;
	@Getter
	private boolean worth = true;

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
}
