package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
