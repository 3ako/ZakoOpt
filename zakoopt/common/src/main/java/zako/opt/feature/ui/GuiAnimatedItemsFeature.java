package zako.opt.feature.ui;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "gui_animated_items",
	group = FeatureGroup.INTERFACE,
	impact = Impact.MEDIUM,
	mixins = {"hud.GuiRendererMixin", "hud.GuiItemAtlasMixin"}
)
public class GuiAnimatedItemsFeature extends Feature {
}
