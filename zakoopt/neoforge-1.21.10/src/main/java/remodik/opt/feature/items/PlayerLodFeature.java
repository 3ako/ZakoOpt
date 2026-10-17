package remodik.opt.feature.items;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

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
