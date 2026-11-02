package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

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
