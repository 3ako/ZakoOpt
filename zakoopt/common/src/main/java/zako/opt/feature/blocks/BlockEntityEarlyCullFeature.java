package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "block_entity_early_cull",
	group = FeatureGroup.BLOCKS,
	impact = Impact.LOW,
	mixins = {"block.BlockEntityRenderDispatcherCullMixin", "block.BlockEntityRenderDispatcherCullWrapMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityEarlyCullFeature extends Feature {
}
