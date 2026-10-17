package remodik.opt.feature.items;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "item_single_copy",
	visual = true,
	group = FeatureGroup.ITEMS,
	enabled = false,
	mixins = "entity.ItemClusterRenderStateMixin"
)
public class ItemSingleCopyFeature extends Feature {
}
