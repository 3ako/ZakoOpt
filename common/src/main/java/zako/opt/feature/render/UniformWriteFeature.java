package zako.opt.feature.render;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// dynamic uniforms are written straight into the coherent persistent mapping; without one the vanilla map/unmap path runs
@FeatureData(
	key = "uniform_write",
	group = FeatureGroup.RENDER,
	mixins = {"gl.GlUniformStorageMixin", "gl.GlBufferAccessor"}
)
public class UniformWriteFeature extends Feature {
}
