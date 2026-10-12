package zako.opt.feature.spawners;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "spawner_replay",
	group = FeatureGroup.SPAWNERS,
	impact = Impact.MEDIUM,
	mixins = "block.SpawnerRendererMixin",
	bench = BenchView.BLOCKS
)
public class SpawnerReplayFeature extends Feature {
}
