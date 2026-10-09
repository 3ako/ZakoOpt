package zako.opt.mixin.gl;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.AnimFreeze;
import zako.opt.gl.ImmediateRing;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
	@Inject(method = "flipFrame", at = @At("HEAD"))
	private static void zakoopt$endFrame(long window, TracyFrameCapture tracy, CallbackInfo ci) {
		ImmediateRing.endFrame();
		AnimFreeze.frame++;
		ZakoOptConfig.refresh();
	}
}
