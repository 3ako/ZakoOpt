package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "lookup_caches",
	group = FeatureGroup.BLOCKS,
	impact = Impact.LOW,
	mixins = {"gl.RenderTypesMixin", "block.PistonHeadRendererMixin"},
	bench = BenchView.OVERVIEW
)
public class LookupCachesFeature extends Feature {
}
