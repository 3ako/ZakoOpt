package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "writer_cache",
	group = FeatureGroup.RENDER,
	mixins = "gl.VertexConsumerUtilsMixin",
	bench = BenchView.CROWD
)
public class WriterCacheFeature extends Feature {
}
