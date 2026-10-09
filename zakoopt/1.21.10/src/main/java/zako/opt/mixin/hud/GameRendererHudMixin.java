package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
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
	private GuiRenderState guiRenderState;
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$hudFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		HudCache.beginFrame(renderLevel);
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$hud(Gui gui, GuiGraphics graphics, DeltaTracker deltaTracker, Operation<Void> original) {
		if (!HudCache.active()) {
			original.call(gui, graphics, deltaTracker);
		} else if (!HudCache.skipExtraction()) {
			HudCache.suppressLiveParts = true;
			try {
				original.call(gui, graphics, deltaTracker);
			} finally {
				HudCache.suppressLiveParts = false;
			}
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
	private void zakoopt$savingIndicator(Gui gui, GuiGraphics graphics, DeltaTracker deltaTracker, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(gui, graphics, deltaTracker);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;render(Lnet/minecraft/client/gui/GuiGraphics;)V"))
	private void zakoopt$toasts(ToastManager toasts, GuiGraphics graphics, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(toasts, graphics);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderDebugOverlay(Lnet/minecraft/client/gui/GuiGraphics;)V"))
	private void zakoopt$debugOverlay(Gui gui, GuiGraphics graphics, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(gui, graphics);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderDeferredSubtitles()V"))
	private void zakoopt$subtitles(Gui gui, Operation<Void> original) {
		if (!HudCache.skipExtraction()) {
			original.call(gui);
		}
	}

	// refresh frames: draw the extracted HUD into the texture; every frame: live parts on screen, then the cached HUD over them
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void zakoopt$drawHud(GuiRenderer renderer, GpuBufferSlice fog, Operation<Void> original, DeltaTracker deltaTracker, boolean renderLevel) {
		if (!HudCache.active()) {
			original.call(renderer, fog);
			return;
		}
		if (!HudCache.skipExtraction()) {
			HudCache.drawIntoTexture(() -> original.call(renderer, fog));
		}
		if (!minecraft.options.hideGui) {
			GuiGraphics graphics = new GuiGraphics(minecraft, guiRenderState);
			GuiInvoker gui = (GuiInvoker) minecraft.gui;
			gui.zakoopt$renderCameraOverlays(graphics, deltaTracker);
			gui.zakoopt$renderCrosshair(graphics, deltaTracker);
			original.call(renderer, fog);
		}
		HudCache.composite();
	}
}
