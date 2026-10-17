package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "frame_fence",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderMixin",
	bench = BenchView.OVERVIEW
)
public class FrameFenceFeature extends Feature {
}
