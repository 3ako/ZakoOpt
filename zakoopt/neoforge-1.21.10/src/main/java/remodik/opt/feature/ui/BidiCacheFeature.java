package remodik.opt.feature.ui;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "bidi_cache",
	group = FeatureGroup.INTERFACE,
	mixins = "text.ClientLanguageMixin",
	bench = BenchView.HOLOGRAMS
)
public class BidiCacheFeature extends Feature {
}
