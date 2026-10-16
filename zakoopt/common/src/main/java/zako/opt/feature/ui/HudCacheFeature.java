package zako.opt.feature.ui;

import zako.opt.MixinPlugin;
import zako.opt.ZakoOptConfig;
import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;
import zako.opt.hud.HudWorth;

import java.util.Objects;

@FeatureData(
	key = "hud_cache",
	group = FeatureGroup.INTERFACE,
	impact = Impact.HIGH,
	mixins = {"hud.GuiRendererMixin", "hud.GuiRendererTargetMixin", "hud.GuiHudMixin"},
	bench = BenchView.OVERVIEW
)
public class HudCacheFeature extends Feature {
	@Override
	public String blocked() {
		return MixinPlugin.VULKAN ? "VulkanMod" : ZakoOptConfig.FOREIGN_HUD ? ZakoOptConfig.FOREIGN_HUD_MOD
				: HudWorth.isMainSwapped() ? Objects.requireNonNullElse(HudWorth.getSwappedBy(), "zakoopt.blocked.other_mod") : null;
	}
}
