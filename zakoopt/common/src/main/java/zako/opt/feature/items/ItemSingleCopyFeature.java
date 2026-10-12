package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "item_single_copy",
	group = FeatureGroup.ITEMS,
	impact = Impact.MEDIUM,
	enabled = false,
	mixins = "entity.ItemClusterRenderStateMixin"
)
public class ItemSingleCopyFeature extends Feature {
}
