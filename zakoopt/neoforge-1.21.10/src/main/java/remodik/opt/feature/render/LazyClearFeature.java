package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "lazy_clear",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderClearMixin",
	bench = BenchView.OVERVIEW
)
public class LazyClearFeature extends Feature {
}
