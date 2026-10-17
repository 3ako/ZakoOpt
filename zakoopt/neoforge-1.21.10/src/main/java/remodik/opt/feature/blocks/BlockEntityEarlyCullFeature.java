package remodik.opt.feature.blocks;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_early_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockEntityRenderDispatcherCullMixin", "block.BlockEntityRenderDispatcherCullWrapMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityEarlyCullFeature extends Feature {
}
