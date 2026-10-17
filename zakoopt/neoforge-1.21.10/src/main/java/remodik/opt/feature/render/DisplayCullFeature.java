package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "display_cull",
	group = FeatureGroup.RENDER,
	mixins = "entity.DisplayCullMixin",
	bench = BenchView.CROWD
)
public class DisplayCullFeature extends Feature {
}
