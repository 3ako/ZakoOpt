package zako.opt.feature.render;

import zako.opt.ZakoOptConfig;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// rain and snow columns are built row by row on the workers; the lists are merged in the same order vanilla produces
@FeatureData(
	key = "parallel_weather",
	group = FeatureGroup.RENDER,
	mixins = {"render.WeatherEffectRendererMixin", "render.WeatherEffectRendererInvoker"}
)
public class ParallelWeatherFeature extends Feature {
	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : null;
	}
}
