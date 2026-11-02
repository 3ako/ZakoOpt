package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "crack_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.LevelRendererCrackMixin"
)
public class CrackCacheFeature extends Feature {
}
