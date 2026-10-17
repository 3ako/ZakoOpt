package remodik.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import remodik.opt.feature.Features;
import remodik.opt.gl.GpuTimers;

// the frame reaches the window through a full-screen shader pass; a framebuffer blit copies the same pixels without one
@Mixin(RenderTarget.class)
public class RenderTargetBlitMixin {
	@Unique
	private static int zakoopt$fbo;

	@Inject(method = "blitToScreen", at = @At("HEAD"), cancellable = true)
	private void zakoopt$blit(CallbackInfo ci) {
		GpuTimers.mark("blit to window");
		RenderTarget self = (RenderTarget) (Object) this;
		if (!Features.FAST_BLIT.on || !(self.getColorTexture() instanceof GlTexture color)) {
			return;
		}
		int prevRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING), prevDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
		if (zakoopt$fbo == 0) {
			zakoopt$fbo = GL30C.glGenFramebuffers();
		}
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, zakoopt$fbo);
		// attached every time: a resize deletes the texture, and GL may hand its name to the new one while the old stays attached here
		GL30C.glFramebufferTexture2D(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, color.glId(), 0);
		boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
		GL30C.glBlitFramebuffer(0, 0, self.width, self.height, 0, 0, self.width, self.height, GL11C.GL_COLOR_BUFFER_BIT, GL11C.GL_NEAREST);
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevRead);
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevDraw);
		if (scissor) {
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
		}
		ci.cancel();
	}
}
