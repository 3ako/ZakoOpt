package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
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

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlobalSettingsUniform;update(IIDJFILnet/minecraft/world/phys/Vec3;Z)V"))
	private void zakoopt$scaleGlobals(GlobalSettingsUniform uniform, int width, int height, double glintAlpha, long gameTime, float worldPartialTicks,
			int menuBlurRadius, Vec3 cameraPos, boolean useRgss, Operation<Void> original) {
		original.call(uniform, width, height, glintAlpha, gameTime, worldPartialTicks, menuBlurRadius, cameraPos, useRgss);
		RenderScale.globals((low, w, h) -> original.call(low, w, h, glintAlpha, gameTime, worldPartialTicks, menuBlurRadius, cameraPos, useRgss));
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel()V"))
	private void zakoopt$scaleBegin(CallbackInfo ci) {
		RenderScale.begin(minecraft);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel()V", shift = At.Shift.AFTER))
	private void zakoopt$scaleEnd(CallbackInfo ci) {
		RenderScale.end(minecraft);
	}
}
