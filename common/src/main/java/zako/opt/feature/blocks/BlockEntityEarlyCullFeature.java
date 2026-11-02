package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_early_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockEntityRenderDispatcherCullMixin", "block.BlockEntityRenderDispatcherCullWrapMixin", "block.NeoBlockEntityRenderDispatcherMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityEarlyCullFeature extends Feature {
}
