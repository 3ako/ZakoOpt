package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "fbo_share",
	group = FeatureGroup.RENDER,
	impact = Impact.LOW,
	mixins = {"gl.GlCommandEncoderDepthClearMixin", "gl.GlTextureViewMixin"},
	bench = BenchView.OVERVIEW
)
public class FboShareFeature extends Feature {
}
