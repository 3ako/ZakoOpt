package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "lazy_clear",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderClearMixin",
	bench = BenchView.OVERVIEW
)
public class LazyClearFeature extends Feature {
}
