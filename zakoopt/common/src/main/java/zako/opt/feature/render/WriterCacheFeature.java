package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "writer_cache",
	group = FeatureGroup.RENDER,
	impact = Impact.LOW,
	mixins = "gl.VertexConsumerUtilsMixin",
	bench = BenchView.CROWD
)
public class WriterCacheFeature extends Feature {
}
