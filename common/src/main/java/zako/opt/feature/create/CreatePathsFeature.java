package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "create_paths",
	group = FeatureGroup.CREATE,
	mixins = "block.CreateFactoryPanelMixin"
)
public class CreatePathsFeature extends Feature {
}
