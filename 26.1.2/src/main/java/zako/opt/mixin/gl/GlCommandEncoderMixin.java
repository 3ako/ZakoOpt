package zako.opt.mixin.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.gl.FrameFence;
import zako.opt.gl.RealFence;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
// 26.1 still makes a GL sync per fence; from 26.2 vanilla shares one per submit and waits on it by submit index
public class GlCommandEncoderMixin {
	@Inject(method = "createFence", at = @At("HEAD"), cancellable = true)
	private void zakoopt$frameFence(CallbackInfoReturnable<GpuFence> cir) {
		if (FrameFence.enabled() && !RealFence.creating) {
			cir.setReturnValue(FrameFence.create());
		}
	}
}
