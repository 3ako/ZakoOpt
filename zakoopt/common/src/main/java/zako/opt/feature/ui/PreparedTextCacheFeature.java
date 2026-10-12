package zako.opt.feature.ui;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "prepared_text_cache",
	group = FeatureGroup.INTERFACE,
	impact = Impact.MEDIUM,
	mixins = "text.FontMixin",
	bench = BenchView.HOLOGRAMS
)
public class PreparedTextCacheFeature extends Feature {
}
