package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Shadow
	private RenderTarget entityTarget;
	@Shadow
	private PostChain entityEffect;
	@Unique
	private boolean zakoopt$glowing = true;

	// the outline target is cleared before the entity loop, so it is the last frame's glowing that left anything in it
	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V"))
	private void zakoopt$clearOutline(RenderTarget target, boolean onOsx, Operation<Void> original) {
		if (target != entityTarget) {
			original.call(target, onOsx);
			return;
		}
		if (zakoopt$glowing || !Features.OUTLINE_SKIP.on) {
			original.call(target, onOsx);
		}
		zakoopt$glowing = false;
	}

	// the outline effect runs only when something was drawn with an outline (on NeoForge custom outlines too)
	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;process(F)V"))
	private void zakoopt$outlineDrawn(PostChain chain, float delta, Operation<Void> original) {
		if (chain == entityEffect) {
			zakoopt$glowing = true;
		}
		original.call(chain, delta);
	}

	// vanilla blends the outline target over the frame even when nothing was drawn into it
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void zakoopt$skipEmptyOutline(CallbackInfo ci) {
		if (!zakoopt$glowing && Features.OUTLINE_SKIP.on) {
			ci.cancel();
		}
	}
}
