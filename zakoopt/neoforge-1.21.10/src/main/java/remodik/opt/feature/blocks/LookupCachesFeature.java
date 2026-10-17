package remodik.opt.feature.blocks;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "lookup_caches",
	group = FeatureGroup.BLOCKS,
	mixins = {"gl.RenderTypesMixin", "block.PistonHeadRendererMixin"},
	bench = BenchView.OVERVIEW
)
public class LookupCachesFeature extends Feature {
}
