package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_addresses",
	group = FeatureGroup.CREATE,
	mixins = "block.CreatePackageAddressMixin"
)
public class CreateAddressesFeature extends Feature {
}
