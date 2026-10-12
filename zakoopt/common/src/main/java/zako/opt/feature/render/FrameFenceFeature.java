package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "frame_fence",
	group = FeatureGroup.RENDER,
	impact = Impact.HIGH,
	mixins = "gl.GlCommandEncoderMixin",
	bench = BenchView.OVERVIEW
)
public class FrameFenceFeature extends Feature {
}
