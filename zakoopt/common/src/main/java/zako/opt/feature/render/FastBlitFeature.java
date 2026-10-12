package zako.opt.feature.render;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "fast_blit",
	group = FeatureGroup.RENDER,
	impact = Impact.LOW,
	mixins = "gl.RenderTargetBlitMixin"
)
public class FastBlitFeature extends Feature {
}
