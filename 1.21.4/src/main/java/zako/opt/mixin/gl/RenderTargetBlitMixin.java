package zako.opt.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.feature.Features;
import zako.opt.gl.GpuTimers;

// the frame reaches the window through a full-screen shader pass; a framebuffer blit copies the same pixels without one
@Mixin(RenderTarget.class)
public class RenderTargetBlitMixin {
	@Inject(method = "blitToScreen(II)V", at = @At("HEAD"), cancellable = true)
	private void zakoopt$blit(int width, int height, CallbackInfo ci) {
		GpuTimers.mark("blit to window");
		RenderTarget self = (RenderTarget) (Object) this;
		if (!Features.FAST_BLIT.on || self.width != width || self.height != height) {
			return;
		}
		int prevDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
		// on 1.21.4 this draws into whatever is bound, not always the window: Resolution Control copies its own target into the
		// game's main one through it, and a blit to the window there left the world black
		if (prevDraw != 0) {
			return;
		}
		int prevRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
		boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
		GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, self.frameBufferId);
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
		GL30C.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11C.GL_COLOR_BUFFER_BIT, GL11C.GL_NEAREST);
		GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevRead);
		GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevDraw);
		if (scissor) {
			GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
		}
		ci.cancel();
	}
}
