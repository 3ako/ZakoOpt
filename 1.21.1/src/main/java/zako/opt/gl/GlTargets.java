package zako.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;

// the GL names behind a render target, for the code that draws with GL directly; -1: not a GL texture
@UtilityClass
public class GlTargets {
	// blaze3d binds no sampler objects before 1.21.5, so there are none to take off a unit and put back
	public final boolean SAMPLERS = false;

	public int depth(RenderTarget target) {
		return target.getDepthTextureId();
	}
}
