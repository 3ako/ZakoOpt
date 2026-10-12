package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "outline_skip",
	group = FeatureGroup.RENDER,
	impact = Impact.LOW,
	mixins = "entity.LevelRendererOutlineMixin",
	bench = BenchView.CROWD
)
public class OutlineSkipFeature extends Feature {
}
