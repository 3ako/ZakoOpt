package zako.opt.mixin.gl;

import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Sodium's terrain shader bound its samplers raw; routing them through the shared cache keeps it honest and skips the
// repeated binds on every chunk draw
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap = false)
public class SodiumShaderSamplerMixin {
	@WrapOperation(method = "bindTexture", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindSampler(II)V"))
	private void zakoopt$sampler(int unit, int sampler, Operation<Void> original) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.samplerCache && GlCaches.sameSampler(unit, sampler)) {
			return;
		}
		original.call(unit, sampler);
	}
}
