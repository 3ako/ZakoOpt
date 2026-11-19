package zako.opt.mixin.gl;

import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// every draw bound a sampler object to each of its units, even when that unit already held it
@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderSamplerMixin {
	@WrapOperation(method = "trySetup", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindSampler(II)V"))
	private void zakoopt$sampler(int unit, int sampler, Operation<Void> original) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.samplerCache && GlCaches.sameSampler(unit, sampler)) {
			return;
		}
		original.call(unit, sampler);
	}
}
