package remodik.opt.feature.blocks;

import remodik.opt.feature.Feature;
import remodik.opt.feature.Features;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

// switching it on or off needs the chunks meshed again; a new distance is picked up as the camera moves
@FeatureData(
	key = "far_leaves",
	visual = true,
	group = FeatureGroup.BLOCKS,
	enabled = false,
	mixins = "block.SodiumFarLeavesMixin"
)
public class FarLeavesFeature extends Feature {
	@Setting(key = "far_leaves_distance")
	@Range(min = 16, max = 128)
	public int distance = 32;

	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		if (was != on) {
			Features.terrainChanged = true;
		}
	}
}
