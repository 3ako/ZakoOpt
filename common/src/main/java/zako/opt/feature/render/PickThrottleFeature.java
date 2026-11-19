package zako.opt.feature.render;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "pick_throttle",
	group = FeatureGroup.RENDER,
	mixins = "gl.GameRendererPickMixin"
)
public class PickThrottleFeature extends Feature {
}
