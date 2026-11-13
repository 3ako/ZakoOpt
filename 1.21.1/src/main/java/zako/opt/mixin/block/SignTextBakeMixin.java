package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

// HangingSignRenderer inherits renderSignWithText and renderSignText
@Mixin(SignRenderer.class)
public class SignTextBakeMixin {
	@Inject(method = "renderSignWithText", at = @At("HEAD"))
	private void zakoopt$begin(SignBlockEntity sign, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, BlockState state, SignBlock block,
							   WoodType wood, Model model, CallbackInfo ci) {
		SignTextMesh.begin(sign, light);
	}

	@Inject(method = "renderSignWithText", at = @At("TAIL"))
	private void zakoopt$end(CallbackInfo ci) {
		SignTextMesh.end();
	}

	@Inject(method = "renderSignText", at = @At("HEAD"), cancellable = true)
	private void zakoopt$baked(CallbackInfo ci) {
		if (SignTextMesh.skip()) {
			ci.cancel();
		}
	}

	// the board of a baked sign is captured with the outline it has up close; how far it reaches is decided at draw time
	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SignRenderer;isOutlineVisible(Lnet/minecraft/core/BlockPos;I)Z"))
	private boolean zakoopt$outline(BlockPos pos, int color, Operation<Boolean> original) {
		return SignTextMesh.capturing() || original.call(pos, color);
	}

	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"))
	private int zakoopt$capture(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
								Font.DisplayMode mode, int background, int light, Operation<Integer> original) {
		SignTextMesh.line(pose, x, y, text, light, color, 0);
		return original.call(font, text, x, y, color, shadow, pose, buffers, mode, background, light);
	}

	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch8xOutline(Lnet/minecraft/util/FormattedCharSequence;FFIILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void zakoopt$captureOutline(Font font, FormattedCharSequence text, float x, float y, int color, int outline, Matrix4f pose, MultiBufferSource buffers,
									   int light, Operation<Void> original) {
		SignTextMesh.line(pose, x, y, text, light, color, outline);
		original.call(font, text, x, y, color, outline, pose, buffers, light);
	}
}
