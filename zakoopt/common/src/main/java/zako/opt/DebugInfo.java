package zako.opt;

import lombok.experimental.UtilityClass;
import zako.opt.gl.RingAutoTune;
import zako.opt.hud.HudWorth;

import java.util.List;

@UtilityClass
public class DebugInfo {
	private List<ZakoOptions.Toggle> available;

	public String line() {
		// available() asks the class loader for a resource, so the list is resolved once
		if (available == null) {
			available = ZakoOptions.PAGES.stream().flatMap(p -> p.groups().stream()).flatMap(g -> g.options().stream())
					.filter(o -> o instanceof ZakoOptions.Toggle && o.available()).map(o -> (ZakoOptions.Toggle) o).toList();
		}
		long on = available.stream().filter(t -> t.get().get()).count();
		String ring = !ZakoOptConfig.values.immediateRing ? "off" : System.getProperty("zakoopt.ring") != null ? (ZakoOptConfig.immediateRing() ? "on" : "off") + " (forced)"
				: ZakoOptConfig.RING_PRESENT ? RingAutoTune.status() : "n/a";
		String hud = ZakoOptConfig.FOREIGN_HUD ? "off (other HUD mod)" : !ZakoOptConfig.values.hudCache ? "off" : HudWorth.isWorth() ? "on" : "idle (FPS too close to monitor rate)";
		return "ZakoOpt " + ZakoOptions.VERSION + ": " + on + "/" + available.size() + " on, ring " + ring + ", HUD cache " + hud;
	}
}
