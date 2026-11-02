package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_items",
	group = FeatureGroup.CREATE,
	mixins = {"block.CreateValueBoxMixin", "block.CreateBeltMixin"}
)
public class CreateItemsFeature extends Feature {
}
