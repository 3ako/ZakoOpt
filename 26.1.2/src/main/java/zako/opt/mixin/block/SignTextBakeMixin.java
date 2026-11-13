package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(AbstractSignRenderer.class)
public class SignTextBakeMixin {
	@Inject(method = "submit", at = @At("HEAD"))
	private void zakoopt$begin(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		SignTextMesh.begin(state, camera.pos);
	}

	@Inject(method = "submit", at = @At("TAIL"))
	private void zakoopt$end(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		SignTextMesh.end();
	}

	@Inject(method = "submitSignText", at = @At("HEAD"), cancellable = true)
	private void zakoopt$baked(SignRenderState state, PoseStack poseStack, SubmitNodeCollector collector, SignText text, CallbackInfo ci) {
		if (SignTextMesh.skip()) {
			ci.cancel();
		}
	}

	@WrapOperation(method = "submitSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitText(Lcom/mojang/blaze3d/vertex/PoseStack;FFLnet/minecraft/util/FormattedCharSequence;ZLnet/minecraft/client/gui/Font$DisplayMode;IIII)V"))
	private void zakoopt$capture(SubmitNodeCollector collector, PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
								 int light, int color, int background, int outline, Operation<Void> original) {
		SignTextMesh.line(poseStack.last().pose(), x, y, text, light, color, outline);
		original.call(collector, poseStack, x, y, text, shadow, mode, light, color, background, outline);
	}
}
