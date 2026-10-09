package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.HudCache;

@Mixin(GameRenderer.class)
public class GameRendererHudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$hudFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		HudCache.beginFrame(renderLevel);
	}

	// refresh frames: the HUD goes into the texture; every frame: live parts on screen, then the cached HUD over them
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$hud(Gui gui, GuiGraphics graphics, DeltaTracker deltaTracker, Operation<Void> original) {
		if (!HudCache.active()) {
			original.call(gui, graphics, deltaTracker);
			return;
		}
		graphics.flush();
		if (!HudCache.skipExtraction()) {
			HudCache.drawIntoTexture(() -> {
				HudCache.suppressLiveParts = true;
				try {
					original.call(gui, graphics, deltaTracker);
					graphics.flush();
				} finally {
					HudCache.suppressLiveParts = false;
				}
			});
		}
		if (!minecraft.options.hideGui) {
			GuiInvoker live = (GuiInvoker) gui;
			live.zakoopt$renderCameraOverlays(graphics, deltaTracker);
			live.zakoopt$renderCrosshair(graphics, deltaTracker);
			graphics.flush();
		}
		HudCache.composite();
	}
}
