package zako.opt.feature.blocks;

import zako.opt.block.Terrain;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

// the faces are left out when chunks are meshed, so a change needs the chunks meshed again
@FeatureData(
	key = "leaf_cull",
	group = FeatureGroup.BLOCKS,
	impact = Impact.HIGH,
	mixins = "block.SodiumLeafCullMixin"
)
public class LeafCullFeature extends Feature {
	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		if (was != on) {
			Terrain.rebuild();
		}
	}
}
