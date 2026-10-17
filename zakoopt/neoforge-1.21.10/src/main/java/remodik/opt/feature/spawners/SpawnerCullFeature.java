package remodik.opt.feature.spawners;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

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
