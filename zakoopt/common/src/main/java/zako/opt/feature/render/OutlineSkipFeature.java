package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "outline_skip",
	group = FeatureGroup.RENDER,
	mixins = "entity.LevelRendererOutlineMixin",
	bench = BenchView.CROWD
)
public class OutlineSkipFeature extends Feature {
}
