package remodik.opt.feature.render;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

// Iris renders the world through its own targets, which would stay at full size
@FeatureData(
	key = "render_scale",
	group = FeatureGroup.RENDER,
	enabled = false,
	mixins = "gl.GameRendererScaleMixin"
)
public class RenderScaleFeature extends Feature {
	@Setting(key = "render_scale_percent")
	@Range(min = 50, max = 90, unit = "zakoopt.percent")
	public int percent = 75;

	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
