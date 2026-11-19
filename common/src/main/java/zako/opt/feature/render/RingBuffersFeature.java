package zako.opt.feature.render;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// one spare slot in every mappable ring: a buffer the GPU is still reading no longer stalls the CPU for a whole frame
@FeatureData(
	key = "ring_buffers",
	group = FeatureGroup.RENDER,
	mixins = {"gl.GlRingBufferMixin"}
)
public class RingBuffersFeature extends Feature {
}
