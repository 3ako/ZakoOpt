package remodik.opt.feature.items;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Range;
import remodik.opt.feature.Setting;

@FeatureData(
	key = "entity_anim_freeze",
	visual = true,
	group = FeatureGroup.ITEMS,
	enabled = false,
	mixins = {"entity.ModelFeatureRendererMixin", "entity.LivingEntityAnimFreezeMixin"}
)
public class EntityAnimFreezeFeature extends Feature {
	@Setting(key = "entity_anim_distance")
	@Range(min = 4, max = 96)
	public int distance = 16;
}
