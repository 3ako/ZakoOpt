package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_couplings",
	group = FeatureGroup.CREATE,
	mixins = "block.CreateCouplingMixin"
)
public class CreateCouplingsFeature extends Feature {
}
