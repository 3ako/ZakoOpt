package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "moving_block_flat_light",
	group = FeatureGroup.BLOCKS,
	impact = Impact.HIGH,
	mixins = "block.NonTerrainBlockRenderContextMixin"
)
public class MovingBlockFlatLightFeature extends Feature {
}
