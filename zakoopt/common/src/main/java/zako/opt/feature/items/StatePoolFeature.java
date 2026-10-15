package zako.opt.feature.items;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;

// the Iris shadow pass extracts the same entities again for its own camera, which would overwrite the pooled state
@FeatureData(
	key = "state_pool",
	group = FeatureGroup.ITEMS,
	mixins = "entity.EntityRenderStatePoolMixin",
	bench = BenchView.CROWD
)
public class StatePoolFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
