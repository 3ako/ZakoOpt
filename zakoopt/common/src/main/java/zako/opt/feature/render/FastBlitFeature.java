package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "fast_blit",
	group = FeatureGroup.RENDER,
	mixins = "gl.RenderTargetBlitMixin",
	bench = BenchView.OVERVIEW
)
public class FastBlitFeature extends Feature {
}
