package zako.opt.feature.spawners;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

@FeatureData(
	key = "spawner_cull",
	visual = true,
	group = FeatureGroup.SPAWNERS,
	mixins = "block.SpawnerRendererMixin"
)
public class SpawnerCullFeature extends Feature {
	@Setting(key = "spawner_distance")
	@Range(min = 4, max = 64)
	public int distance = 16;
}
