package zako.opt.feature.items;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "item_single_copy",
	visual = true,
	group = FeatureGroup.ITEMS,
	enabled = false,
	mixins = {"entity.ItemClusterRenderStateMixin", "entity.ItemEntityRendererMixin"}
)
public class ItemSingleCopyFeature extends Feature {
}
