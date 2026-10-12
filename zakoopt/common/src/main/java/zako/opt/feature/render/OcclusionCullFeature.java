package zako.opt.feature.render;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Impact;

// Iris draws terrain again for its shadow pass with another camera, which the verdicts would not fit
@FeatureData(
	key = "occlusion_cull",
	group = FeatureGroup.RENDER,
	impact = Impact.HIGH,
	enabled = false,
	mixins = "gl.SodiumOcclusionCullMixin"
)
public class OcclusionCullFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
