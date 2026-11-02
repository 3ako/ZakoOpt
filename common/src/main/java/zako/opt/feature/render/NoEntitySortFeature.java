package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "no_entity_sort",
	group = FeatureGroup.RENDER,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.CROWD
)
public class NoEntitySortFeature extends Feature {
}
