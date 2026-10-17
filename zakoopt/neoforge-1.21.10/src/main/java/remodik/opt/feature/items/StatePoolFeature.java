package remodik.opt.feature.items;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;

// the Iris shadow pass extracts the same entities again for its own camera, which would overwrite the pooled state
@FeatureData(
	key = "state_pool",
	group = FeatureGroup.MEMORY,
	mixins = "entity.EntityRenderStatePoolMixin",
	bench = BenchView.CROWD
)
public class StatePoolFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
