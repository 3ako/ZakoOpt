package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "no_item_sort",
	visual = true,
	group = FeatureGroup.RENDER,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.ITEMS
)
public class NoItemSortFeature extends Feature {
}
