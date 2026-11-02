package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "lookup_caches",
	group = FeatureGroup.BLOCKS,
	mixins = {"gl.RenderTypesMixin", "block.PistonHeadRendererMixin"},
	bench = BenchView.OVERVIEW
)
public class LookupCachesFeature extends Feature {
}
