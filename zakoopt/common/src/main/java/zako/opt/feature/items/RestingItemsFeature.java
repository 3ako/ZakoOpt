package zako.opt.feature.items;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "resting_items",
	group = FeatureGroup.ITEMS,
	mixins = "entity.ItemEntityRestMixin",
	bench = BenchView.ITEMS
)
public class RestingItemsFeature extends Feature {
}
