package remodik.opt.feature.ui;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "prepared_text_cache",
	group = FeatureGroup.INTERFACE,
	mixins = "text.FontMixin",
	bench = BenchView.HOLOGRAMS
)
public class PreparedTextCacheFeature extends Feature {
}
