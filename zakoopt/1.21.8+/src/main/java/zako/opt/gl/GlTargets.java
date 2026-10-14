package zako.opt.gl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;

// the GL names behind a render target, for the code that draws with GL directly; -1: not a GL texture (another backend)
@UtilityClass
public class GlTargets {
	// blaze3d binds sampler objects to the texture units: a pass that samples on its own takes them off and puts them back
	public final boolean SAMPLERS = true;

	// blaze3d leaves no render target bound between passes: a GL pass binds its own framebuffer
	public final boolean UNBOUND = true;

	public int color(RenderTarget target) {
		return target.getColorTexture() instanceof GlTexture t ? t.glId() : -1;
	}

	public int depth(RenderTarget target) {
		return target.getDepthTexture() instanceof GlTexture t ? t.glId() : -1;
	}
}
