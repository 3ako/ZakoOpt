package zako.opt.feature.ui;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "gui_animated_items",
	group = FeatureGroup.INTERFACE,
	mixins = {"hud.GuiRendererMixin", "hud.GuiItemAtlasMixin"}
)
public class GuiAnimatedItemsFeature extends Feature {
}
