package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "frame_fence",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderMixin",
	bench = BenchView.OVERVIEW
)
public class FrameFenceFeature extends Feature {
}
