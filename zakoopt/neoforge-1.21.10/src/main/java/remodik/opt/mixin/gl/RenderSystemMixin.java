package remodik.opt.mixin.gl;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import remodik.opt.block.FarLeaves;
import remodik.opt.gl.GpuTimers;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import remodik.opt.ZakoOptConfig;
import remodik.opt.entity.AnimFreeze;
import remodik.opt.entity.SkinAtlas;
import remodik.opt.gl.FrameFence;
import remodik.opt.gl.ImmediateRing;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
	@Inject(method = "flipFrame", at = @At("HEAD"))
	private static void zakoopt$endFrame(Window window, TracyFrameCapture tracy, CallbackInfo ci) {
		GpuTimers.endFrame();
		FarLeaves.frame();
		ImmediateRing.endFrame();
		FrameFence.endFrame();
		SkinAtlas.endFrame();
		AnimFreeze.frame++;
		ZakoOptConfig.refresh();
	}
}
