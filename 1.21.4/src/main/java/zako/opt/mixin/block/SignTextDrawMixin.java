package zako.opt.mixin.block;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(LevelRenderer.class)
public class SignTextDrawMixin {
	// method_62214: the main pass lambda of addMainPass
	@Inject(method = "method_62214", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderBlockEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/Camera;F)V"))
	private void zakoopt$drawSignText(CallbackInfo ci) {
		SignTextMesh.draw();
	}
}
