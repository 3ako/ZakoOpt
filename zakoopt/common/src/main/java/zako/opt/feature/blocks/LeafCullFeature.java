package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.Features;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// the faces are left out when chunks are meshed, so a change needs the chunks meshed again
@FeatureData(
	key = "leaf_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.SodiumLeafCullMixin", "block.SodiumLeafCullOldMixin"}
)
public class LeafCullFeature extends Feature {
	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		if (was != on) {
			Features.terrainChanged = true;
		}
	}
}
