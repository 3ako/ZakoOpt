package zako.opt.feature.create;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

@FeatureData(
	key = "create_far_tick",
	group = FeatureGroup.CREATE,
	mixins = {"block.CreateFarTickMixin", "block.CreateLevelTickersMixin"}
)
public class CreateFarTickFeature extends Feature {
	// from 64 up: block entities are drawn to 64 blocks, so nearer a resting machine would be seen standing still
	@Setting(key = "create_far_tick_distance")
	@Range(min = 64, max = 256)
	public int distance = 80;
}
