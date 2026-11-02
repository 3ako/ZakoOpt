package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

@FeatureData(
	key = "player_lod",
	visual = true,
	group = FeatureGroup.ITEMS,
	mixins = {"entity.AvatarLodMixin", "entity.PlayerLodMixin"}
)
public class PlayerLodFeature extends Feature {
	@Setting(key = "player_lod_distance")
	@Range(min = 8, max = 96)
	public int distance = 35;
}
