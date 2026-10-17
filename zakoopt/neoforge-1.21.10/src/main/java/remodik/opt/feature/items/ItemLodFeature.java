package remodik.opt.feature.items;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

@FeatureData(
	key = "item_lod",
	visual = true,
	group = FeatureGroup.ITEMS,
	mixins = {"entity.ItemRendererMixin", "entity.ItemFeatureRendererMixin"}
)
public class ItemLodFeature extends Feature {
	@Setting(key = "item_lod_distance")
	@Range(min = 4, max = 48)
	public int distance = 8;
}
