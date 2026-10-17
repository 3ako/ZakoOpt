package remodik.opt.feature.blocks;

import remodik.opt.feature.Feature;
import remodik.opt.feature.Features;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

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
