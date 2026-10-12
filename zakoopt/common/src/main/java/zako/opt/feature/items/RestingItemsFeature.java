package zako.opt.feature.items;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "resting_items",
	group = FeatureGroup.ITEMS,
	impact = Impact.MEDIUM,
	mixins = "entity.ItemEntityRestMixin",
	bench = BenchView.ITEMS
)
public class RestingItemsFeature extends Feature {
}
