package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "display_cull",
	group = FeatureGroup.RENDER,
	mixins = "entity.DisplayCullMixin",
	bench = BenchView.CROWD
)
public class DisplayCullFeature extends Feature {
}
