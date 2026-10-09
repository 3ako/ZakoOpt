package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.hud.HudCache;
import zako.opt.hud.MonitorRate;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
	@Unique
	private long zakoopt$lastAnimatedRefresh;
	@Unique
	private boolean zakoopt$refreshAnimated;

	@Inject(method = "prepareItemElements", at = @At("HEAD"))
	private void zakoopt$decideRefresh(CallbackInfo ci) {
		long now = System.nanoTime();
		zakoopt$refreshAnimated = !ZakoOptConfig.guiAnimatedItems() || now - zakoopt$lastAnimatedRefresh >= MonitorRate.intervalNs(now);
		if (zakoopt$refreshAnimated) {
			zakoopt$lastAnimatedRefresh = now;
		}
	}

	// ordinal 0 = the cache-hit check; ordinal 1 only decides whether to reuse the atlas slot
	@ModifyExpressionValue(method = "method_71055", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;isAnimated()Z", ordinal = 0))
	private boolean zakoopt$throttleAnimated(boolean animated) {
		return animated && zakoopt$refreshAnimated;
	}

	@WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private RenderTarget zakoopt$hudTarget(Minecraft minecraft, Operation<RenderTarget> original) {
		RenderTarget target = HudCache.drawTarget();
		return target != null ? target : original.call(minecraft);
	}
}
