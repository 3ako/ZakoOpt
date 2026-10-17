package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "fbo_share",
	group = FeatureGroup.RENDER,
	mixins = {"gl.GlCommandEncoderDepthClearMixin", "gl.GlTextureViewMixin"},
	bench = BenchView.OVERVIEW
)
public class FboShareFeature extends Feature {
}
