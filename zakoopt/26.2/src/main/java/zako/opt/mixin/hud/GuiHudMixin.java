package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.HudCache;

@Mixin(Gui.class)
public class GuiHudMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void zakoopt$hudFrame(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
		HudCache.beginFrame(shouldRenderLevel);
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$hud(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
		if (!HudCache.active()) {
			original.call(hud, graphics, deltaTracker);
		} else if (!HudCache.skipExtraction()) {
			HudCache.suppressLiveParts = true;
			try {
				original.call(hud, graphics, deltaTracker);
			} finally {
				HudCache.suppressLiveParts = false;
			}
		} else {
			// normally set by the skipped extraction; the hand and the 3D crosshair read it
			minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden = hud.isHidden();
		}
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$savingIndicator(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(hud, graphics, deltaTracker);
		}
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void zakoopt$toasts(ToastManager toasts, GuiGraphicsExtractor graphics, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(toasts, graphics);
		}
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDebugOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void zakoopt$debugOverlay(Hud hud, GuiGraphicsExtractor graphics, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(hud, graphics);
		}
	}

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
	private void zakoopt$subtitles(Hud hud, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(hud);
		}
	}
}
