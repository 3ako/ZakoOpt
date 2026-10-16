package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockFeatureRendererMixin", "block.PistonHeadRendererCacheMixin"},
	bench = BenchView.PISTONS
)
public class MovingBlockCacheFeature extends Feature {
}
