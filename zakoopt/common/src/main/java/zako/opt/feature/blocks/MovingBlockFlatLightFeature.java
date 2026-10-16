package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_flat_light",
	visual = true,
	group = FeatureGroup.BLOCKS,
	mixins = "block.NonTerrainBlockRenderContextMixin"
)
public class MovingBlockFlatLightFeature extends Feature {
}
