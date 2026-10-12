package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Impact;

// Iris reads vertices back to build its extended attributes, and reads from the mapped ring are uncached
@FeatureData(
	key = "ring_zero_copy",
	group = FeatureGroup.RENDER,
	impact = Impact.MEDIUM,
	mixins = {"gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"},
	bench = BenchView.OVERVIEW
)
public class RingZeroCopyFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
