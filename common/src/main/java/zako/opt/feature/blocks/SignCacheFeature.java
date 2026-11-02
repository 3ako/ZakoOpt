package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "sign_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.AbstractSignRendererMixin", "block.SignRendererMixin", "block.HangingSignRendererMixin"},
	bench = BenchView.BLOCKS
)
public class SignCacheFeature extends Feature {
}
