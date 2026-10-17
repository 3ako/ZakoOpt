package remodik.opt.feature.render;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;

// Iris draws terrain again for its shadow pass with another camera, which the verdicts would not fit
@FeatureData(
	key = "occlusion_cull",
	group = FeatureGroup.RENDER,
	enabled = false,
	mixins = "gl.SodiumOcclusionCullMixin"
)
public class OcclusionCullFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
