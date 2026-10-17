package remodik.opt.feature.ui;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "gui_animated_items",
	group = FeatureGroup.INTERFACE,
	mixins = {"hud.GuiRendererMixin", "hud.GuiItemAtlasMixin"}
)
public class GuiAnimatedItemsFeature extends Feature {
}
