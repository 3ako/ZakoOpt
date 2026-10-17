package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "lunar_multidraw",
	group = FeatureGroup.RENDER,
	config = "lunarMultiDraw",
	mixins = {"gl.LunarMultiDrawMixin", "gl.LunarSodiumMultiDrawMixin"},
	bench = BenchView.TERRAIN
)
public class LunarMultiDrawFeature extends Feature {
}
