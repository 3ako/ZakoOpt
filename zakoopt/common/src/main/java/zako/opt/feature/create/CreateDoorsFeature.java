package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_doors",
	group = FeatureGroup.CREATE,
	mixins = "block.CreateDoorMixin"
)
public class CreateDoorsFeature extends Feature {
}
