package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "held_item_cache",
	group = FeatureGroup.ITEMS,
	mixins = {"entity.ItemModelResolverMixin", "entity.ItemStackResolveStampMixin"}
)
public class HeldItemCacheFeature extends Feature {
}
