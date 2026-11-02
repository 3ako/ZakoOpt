package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.gl.TexBufferCache;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public class GlCommandEncoderTexBufferMixin {
	@WrapOperation(method = "setupDraw", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindTexture(II)V"))
	private void zakoopt$trackTbo(int target, int texture, Operation<Void> original) {
		TexBufferCache.boundTexture = texture;
		original.call(target, texture);
	}

	// re-attaching the same buffer every draw makes the driver revalidate the texture (clouds: ~2% of a frame in glDrawElements)
	@WrapOperation(method = "setupDraw", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glTexBuffer(III)V"))
	private void zakoopt$texBufferOnce(int target, int format, int buffer, Operation<Void> original) {
		if (Features.MICRO_OPTS.texBufferOnce && TexBufferCache.attached(format, buffer)) {
			return;
		}
		original.call(target, format, buffer);
		TexBufferCache.attach(format, buffer);
	}
}
