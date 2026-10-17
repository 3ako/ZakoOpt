package remodik.opt.feature.blocks;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "sign_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.AbstractSignRendererMixin",
	bench = BenchView.BLOCKS
)
public class SignCacheFeature extends Feature {
}
