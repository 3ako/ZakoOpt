package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "no_item_sort",
	group = FeatureGroup.RENDER,
	impact = Impact.MEDIUM,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.ITEMS
)
public class NoItemSortFeature extends Feature {
}
