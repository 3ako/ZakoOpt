package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.gl.RenderScale;

@Mixin(GameRenderer.class)
public class GameRendererScaleMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlobalSettingsUniform;update(IIDJLnet/minecraft/client/DeltaTracker;ILnet/minecraft/world/phys/Vec3;Z)V"))
	private void zakoopt$scaleGlobals(GlobalSettingsUniform uniform, int width, int height, double glintAlpha, long gameTime, DeltaTracker deltaTracker, int menuBlurRadius, Vec3 cameraPos, boolean useRgss, Operation<Void> original) {
		original.call(uniform, width, height, glintAlpha, gameTime, deltaTracker, menuBlurRadius, cameraPos, useRgss);
		RenderScale.globals((low, w, h) -> original.call(low, w, h, glintAlpha, gameTime, deltaTracker, menuBlurRadius, cameraPos, useRgss));
	}

	// one wrap, so the main target gets its own textures back however the world render ends
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$scale(GameRenderer renderer, DeltaTracker deltaTracker, Operation<Void> original) {
		RenderScale.begin(minecraft);
		try {
			original.call(renderer, deltaTracker);
		} finally {
			RenderScale.end(minecraft);
		}
	}
}
