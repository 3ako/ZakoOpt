package zako.opt.mixin.gl;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.AnimFreeze;
import zako.opt.entity.SkinAtlas;
import zako.opt.gl.FrameFence;
import zako.opt.gl.ImmediateRing;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
	@Inject(method = "flipFrame", at = @At("HEAD"))
	private static void zakoopt$endFrame(Window window, TracyFrameCapture tracy, CallbackInfo ci) {
		ImmediateRing.endFrame();
		FrameFence.endFrame();
		SkinAtlas.endFrame();
		AnimFreeze.frame++;
		ZakoOptConfig.refresh();
	}
}
