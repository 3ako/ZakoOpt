package zako.opt.mixin.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.gl.FrameFence;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
	@Inject(method = "createFence", at = @At("HEAD"), cancellable = true)
	private void zakoopt$frameFence(CallbackInfoReturnable<GpuFence> cir) {
		if (FrameFence.enabled()) {
			cir.setReturnValue(FrameFence.create());
		}
	}
}
