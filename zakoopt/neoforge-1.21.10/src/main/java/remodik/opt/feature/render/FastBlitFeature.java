package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "fast_blit",
	group = FeatureGroup.RENDER,
	mixins = "gl.RenderTargetBlitMixin",
	bench = BenchView.OVERVIEW
)
public class FastBlitFeature extends Feature {
}
