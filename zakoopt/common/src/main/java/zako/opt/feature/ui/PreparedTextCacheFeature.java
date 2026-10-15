package zako.opt.feature.ui;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "prepared_text_cache",
	group = FeatureGroup.INTERFACE,
	mixins = "text.FontMixin",
	bench = BenchView.HOLOGRAMS
)
public class PreparedTextCacheFeature extends Feature {
}
