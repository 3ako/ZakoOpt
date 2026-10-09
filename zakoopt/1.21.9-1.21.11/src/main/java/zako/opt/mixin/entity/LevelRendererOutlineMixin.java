package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Shadow
	@Final
	private LevelRenderState levelRenderState;
	@Unique
	private boolean zakoopt$outlineDirty = true;

	// vanilla clears the outline target every frame; without glowing entities it is already clear from the last frame
	@WrapOperation(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V"))
	private void zakoopt$clearOutline(CommandEncoder encoder, GpuTexture color, int clearColor, GpuTexture depth, double clearDepth, Operation<Void> original) {
		boolean glowing = levelRenderState.haveGlowingEntities;
		if (glowing || zakoopt$outlineDirty || !ZakoOptConfig.outlineSkip()) {
			original.call(encoder, color, clearColor, depth, clearDepth);
		}
		zakoopt$outlineDirty = glowing;
	}

	// ...and blends that empty target over the frame
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void zakoopt$skipEmptyOutline(CallbackInfo ci) {
		if (!levelRenderState.haveGlowingEntities && ZakoOptConfig.outlineSkip()) {
			ci.cancel();
		}
	}
}
