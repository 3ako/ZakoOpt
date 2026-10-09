package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
	@Shadow
	private static int readFbo;
	@Shadow
	private static int writeFbo;

	// vanilla rebinds GL_FRAMEBUFFER as two calls (read, then draw); one call sets both
	@Inject(method = "_glBindFramebuffer", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$singleBind(int target, int fbo, CallbackInfo ci) {
		if (target == GL30.GL_FRAMEBUFFER && readFbo != fbo && writeFbo != fbo && ZakoOptConfig.microOpts()) {
			GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
			readFbo = fbo;
			writeFbo = fbo;
			ci.cancel();
		}
	}
}
