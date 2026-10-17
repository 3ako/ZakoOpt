package remodik.opt.feature.blocks;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_flat_light",
	visual = true,
	group = FeatureGroup.BLOCKS,
	mixins = "block.NonTerrainBlockRenderContextMixin"
)
public class MovingBlockFlatLightFeature extends Feature {
}
