package remodik.opt.gl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;

// the GL names behind a render target, for the code that draws with GL directly; -1: not a GL texture (another backend)
@UtilityClass
public class GlTargets {
	// blaze3d binds sampler objects to the texture units: a pass that samples on its own takes them off and puts them back
	public final boolean SAMPLERS = true;

	public int depth(RenderTarget target) {
		return target.getDepthTexture() instanceof GlTexture t ? t.glId() : -1;
	}
}
