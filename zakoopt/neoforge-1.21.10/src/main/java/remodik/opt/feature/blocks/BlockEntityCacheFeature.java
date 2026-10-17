package remodik.opt.feature.blocks;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.ChestRendererMixin",
	bench = BenchView.BLOCKS
)
public class BlockEntityCacheFeature extends Feature {
}
