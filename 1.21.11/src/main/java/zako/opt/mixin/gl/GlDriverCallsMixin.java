package zako.opt.mixin.gl;

import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// the framebuffer half of this lives in the 1.21.8+ GlStateManagerMixin (micro_opts); this one carries the driver_calls
// parts only
@Mixin(GlStateManager.class)
public class GlDriverCallsMixin {
	@Shadow
	private static int activeTexture;

	// the same vertex array stays bound through a whole batch of draws
	@Inject(method = "_glBindVertexArray", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$sameVao(int vao, CallbackInfo ci) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.vaoCache && GlCaches.sameVao(vao)) {
			ci.cancel();
		}
	}

	// every draw set the same scissor rectangle again (GUI and HUD draw hundreds of them)
	@Inject(method = "_scissorBox", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$sameScissor(int x, int y, int width, int height, CallbackInfo ci) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.scissorCache && GlCaches.sameScissor(x, y, width, height)) {
			ci.cancel();
		}
	}

	// the pipeline's polygon mode is already in the context
	@Inject(method = "_polygonMode", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$samePolygon(int face, int mode, CallbackInfo ci) {
		if (face == GL11C.GL_FRONT_AND_BACK && Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.polygonCache && GlCaches.samePolygon(mode)) {
			ci.cancel();
		}
	}

	// every sampler of every draw set the same two mip limits on the texture it had just bound
	@Inject(method = "_texParameter", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$sameMip(int target, int pname, int value, CallbackInfo ci) {
		if (target == GL11C.GL_TEXTURE_2D && (pname == 33084 || pname == 33085)
				&& Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.mipCache
				&& GlCaches.sameMip(GlCaches.texture(activeTexture), pname, value)) {
			ci.cancel();
		}
	}

	// the mip cache is keyed by texture name: a new texture can be given the name of a deleted one
	@Inject(method = "_bindTexture", at = @At("HEAD"))
	private static void zakoopt$trackTexture(int id, CallbackInfo ci) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.mipCache) {
			GlCaches.texture(activeTexture, id);
		}
	}

	@Inject(method = "_deleteTexture", at = @At("HEAD"))
	private static void zakoopt$forgetTexture(int id, CallbackInfo ci) {
		GlCaches.forgetTexture(id);
	}

	// a buffer name can be reused too, and a new buffer is not necessarily coherent
	@Inject(method = "_glDeleteBuffers", at = @At("HEAD"))
	private static void zakoopt$forgetBuffer(int id, CallbackInfo ci) {
		GlCaches.forgetBuffer(id);
	}
}
