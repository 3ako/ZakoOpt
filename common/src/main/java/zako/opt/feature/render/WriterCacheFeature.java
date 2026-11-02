package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "writer_cache",
	group = FeatureGroup.RENDER,
	mixins = "gl.VertexConsumerUtilsMixin",
	bench = BenchView.CROWD
)
public class WriterCacheFeature extends Feature {
}
