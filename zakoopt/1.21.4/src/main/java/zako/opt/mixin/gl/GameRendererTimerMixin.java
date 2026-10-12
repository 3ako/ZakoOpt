package zako.opt.mixin.gl;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.GpuTimers;

@Mixin(GameRenderer.class)
public class GameRendererTimerMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$beginFrame(CallbackInfo ci) {
		GpuTimers.beginFrame();
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"))
	private void zakoopt$markHand(CallbackInfo ci) {
		GpuTimers.mark("hand");
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$markHud(CallbackInfo ci) {
		GpuTimers.mark("hud");
	}
}
