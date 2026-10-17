package remodik.opt.feature.blocks;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockFeatureRendererMixin", "block.PistonHeadRendererCacheMixin"},
	bench = BenchView.PISTONS
)
public class MovingBlockCacheFeature extends Feature {
}
