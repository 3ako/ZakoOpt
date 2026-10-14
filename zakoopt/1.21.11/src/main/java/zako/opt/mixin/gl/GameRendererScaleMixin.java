package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.RenderScale;

@Mixin(GameRenderer.class)
public class GameRendererScaleMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlobalSettingsUniform;update(IIDJLnet/minecraft/client/DeltaTracker;ILnet/minecraft/client/Camera;Z)V"))
	private void zakoopt$scaleGlobals(GlobalSettingsUniform uniform, int width, int height, double glintAlpha, long gameTime, DeltaTracker deltaTracker,
			int menuBlurRadius, Camera camera, boolean useRgss, Operation<Void> original) {
		original.call(uniform, width, height, glintAlpha, gameTime, deltaTracker, menuBlurRadius, camera, useRgss);
		RenderScale.globals((low, w, h) -> original.call(low, w, h, glintAlpha, gameTime, deltaTracker, menuBlurRadius, camera, useRgss));
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$scaleBegin(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderScale.begin(minecraft);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
	private void zakoopt$scaleEnd(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderScale.end(minecraft);
	}
}
