package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "outline_skip",
	group = FeatureGroup.RENDER,
	mixins = "entity.LevelRendererOutlineMixin",
	bench = BenchView.CROWD
)
public class OutlineSkipFeature extends Feature {
}
