package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(AbstractSignRenderer.class)
public class SignTextBakeMixin {
	@Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
	private void zakoopt$begin(SignBlockEntity sign, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, Vec3 camera, CallbackInfo ci) {
		SignTextMesh.begin(sign, light, camera);
	}

	@Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
	private void zakoopt$end(CallbackInfo ci) {
		SignTextMesh.end();
	}

	@Inject(method = "renderSignText", at = @At("HEAD"), cancellable = true)
	private void zakoopt$baked(CallbackInfo ci) {
		if (SignTextMesh.skip()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSign", at = @At("HEAD"), cancellable = true)
	private void zakoopt$noBoard(CallbackInfo ci) {
		if (SignTextMesh.capturing()) {
			ci.cancel();
		}
	}

	// a baked sign keeps its up-close outline as its own range, drawn by distance later
	@ModifyReturnValue(method = "isOutlineVisible", at = @At("RETURN"))
	private static boolean zakoopt$outline(boolean visible) {
		return visible || SignTextMesh.capturing();
	}

	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V"))
	private void zakoopt$capture(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
								 Font.DisplayMode mode, int background, int light, Operation<Void> original) {
		SignTextMesh.line(pose, x, y, text, light, color, 0);
		if (!SignTextMesh.capturing()) {
			original.call(font, text, x, y, color, shadow, pose, buffers, mode, background, light);
		}
	}

	@WrapOperation(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch8xOutline(Lnet/minecraft/util/FormattedCharSequence;FFIILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void zakoopt$captureOutlined(Font font, FormattedCharSequence text, float x, float y, int color, int outline, Matrix4f pose, MultiBufferSource buffers,
										 int light, Operation<Void> original) {
		SignTextMesh.line(pose, x, y, text, light, color, outline);
		if (!SignTextMesh.capturing()) {
			original.call(font, text, x, y, color, outline, pose, buffers, light);
		}
	}
}
