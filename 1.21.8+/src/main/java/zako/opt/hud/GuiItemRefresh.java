package zako.opt.hud;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GuiItemRefresh {
	private long last;
	private boolean refresh = true;

	public void decide() {
		long now = System.nanoTime();
		// the HUD cache already redraws at the monitor rate; throttling on top of it left enchanted hotbar items blank
		refresh = !Features.GUI_ANIMATED_ITEMS.on || HudCache.active() || now - last >= MonitorRate.intervalNs(now);
		if (refresh) {
			last = now;
		}
	}

	public boolean refresh() {
		return refresh;
	}
}
