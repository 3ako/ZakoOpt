package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.TexBufferCache;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderTexBufferMixin {
	@WrapOperation(method = "trySetup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindTexture(II)V"))
	private void zakoopt$trackTbo(int target, int texture, Operation<Void> original) {
		TexBufferCache.boundTexture = texture;
		original.call(target, texture);
	}

	// re-attaching the same buffer every draw makes the driver revalidate the texture (clouds: ~2% of a frame in glDrawElements)
	@WrapOperation(method = "trySetup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glTexBuffer(III)V"))
	private void zakoopt$texBufferOnce(int target, int format, int buffer, Operation<Void> original) {
		if (ZakoOptConfig.tboCache() && TexBufferCache.attached(format, buffer)) {
			return;
		}
		original.call(target, format, buffer);
		TexBufferCache.attach(format, buffer);
	}
}
