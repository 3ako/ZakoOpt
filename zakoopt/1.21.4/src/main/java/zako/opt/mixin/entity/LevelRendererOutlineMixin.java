package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Unique
	private boolean zakoopt$glowing = true;
	@Unique
	private boolean zakoopt$outlineDirty = true;

	// true when a visible entity glows, i.e. the outline target gets drawn into this frame
	@ModifyReturnValue(method = "collectVisibleEntities", at = @At("RETURN"))
	private boolean zakoopt$glowing(boolean glowing) {
		zakoopt$glowing = glowing;
		return glowing;
	}

	// vanilla clears the outline target every frame; without glowing entities it is already clear from the last frame
	@WrapOperation(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear()V"))
	private void zakoopt$clearOutline(RenderTarget target, Operation<Void> original) {
		if (zakoopt$glowing || zakoopt$outlineDirty || !ZakoOptConfig.outlineSkip()) {
			original.call(target);
		}
		zakoopt$outlineDirty = zakoopt$glowing;
	}

	// ...and blends that empty target over the frame
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void zakoopt$skipEmptyOutline(CallbackInfo ci) {
		if (!zakoopt$glowing && ZakoOptConfig.outlineSkip()) {
			ci.cancel();
		}
	}
}
