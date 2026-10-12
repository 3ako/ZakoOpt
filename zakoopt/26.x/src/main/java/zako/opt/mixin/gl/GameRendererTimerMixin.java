package zako.opt.mixin.gl;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.GpuTimers;

// targets without descriptors: the arguments of these calls differ between 26.1 and 26.3
@Mixin(GameRenderer.class)
public class GameRendererTimerMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$beginFrame(CallbackInfo ci) {
		GpuTimers.beginFrame();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Lightmap;render"))
	private void zakoopt$markLightmap(CallbackInfo ci) {
		GpuTimers.mark("lightmap");
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel"))
	private void zakoopt$markLevel(CallbackInfo ci) {
		GpuTimers.mark("level setup");
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"))
	private void zakoopt$markHand(CallbackInfo ci) {
		GpuTimers.mark("hand");
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render"))
	private void zakoopt$markHud(CallbackInfo ci) {
		GpuTimers.mark("hud");
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void zakoopt$endFrame(CallbackInfo ci) {
		GpuTimers.endFrame();
	}
}
