package zako.opt.feature.render;

import zako.opt.MixinPlugin;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;

// driver calls that repeated the state already in the context: texture mip limits, scissor box, polygon mode, the bound
// vertex array and the sampler of each unit. Each part keeps its own -Dzakoopt.driver_calls.<part> switch
@FeatureData(
	key = "driver_calls",
	group = FeatureGroup.RENDER
)
public class DriverCallsFeature extends Feature {
	public boolean mipCache;
	public boolean scissorCache;
	public boolean polygonCache;
	public boolean vaoCache;
	public boolean samplerCache;
	// buffers created while this is on are mapped coherent, so their writes need no glFlushMappedBufferRange
	public boolean coherentBuffers;

	@Override
	public String blocked() {
		// Iris binds textures, samplers, vertex arrays and the polygon mode with raw calls of its own
		return MixinPlugin.VULKAN ? "VulkanMod" : Features.shaders() ? "Iris" : null;
	}

	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		mipCache = flag("driver_calls.mip_cache", value);
		scissorCache = flag("driver_calls.scissor_cache", value);
		polygonCache = flag("driver_calls.polygon_cache", value);
		vaoCache = flag("driver_calls.vao_cache", value);
		samplerCache = flag("driver_calls.sampler_cache", value);
		coherentBuffers = flag("driver_calls.coherent_buffers", value);
		if (was != on) {
			GlCaches.reset();
		}
	}
}
