package zako.opt.mixin.block;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(LevelRenderer.class)
public class SignTextDrawMixin {
	// the main pass's first plain endBatch() flushes the block entities' text, right before the translucent terrain
	@Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", ordinal = 0, shift = At.Shift.AFTER))
	private void zakoopt$drawSignText(CallbackInfo ci) {
		SignTextMesh.draw();
	}
}
