package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderClearMixin {
	// the next render pass binds its own target anyway; GlStateManager's cache then skips the redundant bind
	@WrapOperation(method = "clearColorAndDepthTextures", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_glBindFramebuffer(II)V"))
	private void zakoopt$keepBound(int target, int framebuffer, Operation<Void> original) {
		if (framebuffer != 0 || !ZakoOptConfig.lazyClear()) {
			original.call(target, framebuffer);
		}
	}
}
