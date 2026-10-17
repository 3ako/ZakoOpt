package remodik.opt.feature.spawners;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "spawner_tick_cache",
	group = FeatureGroup.SPAWNERS,
	mixins = "block.SpawnerRendererMixin",
	bench = BenchView.BLOCKS
)
public class SpawnerTickCacheFeature extends Feature {
}
