package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.ChestRendererMixin",
	bench = BenchView.BLOCKS
)
public class BlockEntityCacheFeature extends Feature {
}
