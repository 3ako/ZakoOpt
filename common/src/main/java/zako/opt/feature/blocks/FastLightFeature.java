package zako.opt.feature.blocks;

import zako.opt.ZakoOptConfig;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// chunk loading: the sky light columns are scanned on the workers and the light queue is drained under the loading
// overlay. Neither changes what the world looks like once the chunk is lit
@FeatureData(
	key = "fast_light",
	group = FeatureGroup.BLOCKS,
	mixins = {"chunk.ChunkSkyLightSourcesMixin", "chunk.ClientLevelLightMixin"}
)
public class FastLightFeature extends Feature {
	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : null;
	}
}
