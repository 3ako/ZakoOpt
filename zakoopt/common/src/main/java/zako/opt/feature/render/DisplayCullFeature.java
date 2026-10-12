package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "display_cull",
	group = FeatureGroup.RENDER,
	impact = Impact.MEDIUM,
	mixins = "entity.DisplayCullMixin",
	bench = BenchView.CROWD
)
public class DisplayCullFeature extends Feature {
}
