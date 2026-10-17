package remodik.opt.feature.items;

import remodik.opt.ZakoOptConfig;
import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;

// Iris builds its extended entity attributes from the vertices it is handed one by one
@FeatureData(
	key = "parallel_models",
	group = FeatureGroup.ITEMS,
	mixins = {"entity.ModelParallelMixin", "entity.ModelFeatureRendererMixin"},
	bench = BenchView.CROWD
)
public class ParallelModelsFeature extends Feature {
	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : Features.shaders() ? "Iris" : null;
	}
}
