package remodik.opt;

import remodik.opt.feature.Features;

import lombok.experimental.UtilityClass;
import remodik.opt.gl.RingAutoTune;
import remodik.opt.hud.HudWorth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@UtilityClass
public class DebugInfo {
	private List<ZakoOptions.Toggle> available;
	// more F3 lines from version code that has them to show
	public final List<Supplier<List<String>>> EXTRA = new CopyOnWriteArrayList<>();

	public List<String> lines() {
		List<String> lines = new ArrayList<>();
		lines.add(line());
		lines.addAll(Guard.lines());
		EXTRA.forEach(extra -> lines.addAll(extra.get()));
		return lines;
	}

	public String line() {
		// available() asks the class loader for a resource, so the list is resolved once
		if (available == null) {
			available = ZakoOptions.GROUPS.stream().flatMap(g -> g.options().stream())
					.filter(o -> o instanceof ZakoOptions.Toggle && o.available()).map(o -> (ZakoOptions.Toggle) o).toList();
		}
		long on = available.stream().filter(t -> t.get().get()).count();
		String ring = !Features.IMMEDIATE_RING.value ? "off" : Features.IMMEDIATE_RING.forced() ? (Features.IMMEDIATE_RING.on ? "on" : "off") + " (forced)"
				: ZakoOptConfig.RING_PRESENT ? RingAutoTune.status() : "n/a";
		String hud = MixinPlugin.VULKAN ? "off (VulkanMod)" : ZakoOptConfig.FOREIGN_HUD ? "off (" + ZakoOptConfig.FOREIGN_HUD_MOD + ")" : HudWorth.isMainSwapped() ? "off (main target swapped by " + Objects.requireNonNullElse(HudWorth.getSwappedBy(), "another mod") + ")" : !Features.HUD_CACHE.value ? "off" : HudWorth.isWorth() ? "on" : "idle (FPS too close to monitor rate)";
		if (!ZakoOptConfig.enabled) {
			return "ZakoOpt " + ZakoOptions.VERSION + ": off";
		}
		return "ZakoOpt " + ZakoOptions.VERSION + ": " + on + "/" + available.size() + " on, ring " + ring + ", HUD cache " + hud + ", cores " + remodik.opt.cpu.CorePin.status();
	}
}
