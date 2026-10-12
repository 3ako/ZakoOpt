package zako.opt.feature.ui;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "bidi_cache",
	group = FeatureGroup.INTERFACE,
	impact = Impact.LOW,
	mixins = "text.ClientLanguageMixin",
	bench = BenchView.HOLOGRAMS
)
public class BidiCacheFeature extends Feature {
}
