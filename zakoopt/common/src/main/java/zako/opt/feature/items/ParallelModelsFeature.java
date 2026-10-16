package zako.opt.feature.items;

import zako.opt.ZakoOptConfig;
import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;

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
