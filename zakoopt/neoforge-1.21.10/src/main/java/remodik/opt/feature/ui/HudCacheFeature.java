package remodik.opt.feature.ui;

import remodik.opt.MixinPlugin;
import remodik.opt.ZakoOptConfig;
import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.hud.HudWorth;

import java.util.Objects;

@FeatureData(
	key = "hud_cache",
	group = FeatureGroup.INTERFACE,
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
