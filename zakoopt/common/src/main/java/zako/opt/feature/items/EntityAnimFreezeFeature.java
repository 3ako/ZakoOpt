package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

@FeatureData(
	key = "entity_anim_freeze",
	group = FeatureGroup.ITEMS,
	impact = Impact.HIGH,
	enabled = false,
	mixins = {"entity.ModelFeatureRendererMixin", "entity.LivingEntityAnimFreezeMixin"}
)
public class EntityAnimFreezeFeature extends Feature {
	@Setting(key = "entity_anim_distance")
	@Range(min = 4, max = 96)
	public int distance = 16;
}
