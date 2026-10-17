package remodik.opt.feature.items;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "resting_items",
	group = FeatureGroup.ITEMS,
	mixins = "entity.ItemEntityRestMixin",
	bench = BenchView.ITEMS
)
public class RestingItemsFeature extends Feature {
}
