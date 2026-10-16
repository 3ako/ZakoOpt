package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "lunar_multidraw",
	group = FeatureGroup.RENDER,
	config = "lunarMultiDraw",
	mixins = {"gl.LunarMultiDrawMixin", "gl.LunarSodiumMultiDrawMixin"},
	bench = BenchView.TERRAIN
)
public class LunarMultiDrawFeature extends Feature {
}
