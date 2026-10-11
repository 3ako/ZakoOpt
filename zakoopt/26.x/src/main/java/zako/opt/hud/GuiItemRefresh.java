package zako.opt.hud;

import lombok.experimental.UtilityClass;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class GuiItemRefresh {
	private long last;
	private boolean refresh = true;

	public void decide() {
		long now = System.nanoTime();
		// the HUD cache already redraws at the monitor rate; throttling on top of it left enchanted hotbar items blank
		refresh = !ZakoOptConfig.guiAnimatedItems() || HudCache.active() || now - last >= MonitorRate.intervalNs(now);
		if (refresh) {
			last = now;
		}
	}

	public boolean refresh() {
		return refresh;
	}
}
