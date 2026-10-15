package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.Features;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

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
