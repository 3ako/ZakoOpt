package zako.opt.mixin.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.DrawStats;

@Mixin(GlStateManager.class)
public class GlStateManagerStatsMixin {
	// only binds that reach GL; repeated binds of the same framebuffer are already skipped above this call
	@Inject(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V"))
	private static void zakoopt$countBind(int target, int fbo, CallbackInfo ci) {
		DrawStats.bind();
	}
}
