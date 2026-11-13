package zako.opt.mixin.block;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(LevelRenderer.class)
public class SignTextArmMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void zakoopt$arm(CallbackInfo ci) {
		SignTextMesh.arm();
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"))
	private void zakoopt$drawSignText(CallbackInfo ci) {
		SignTextMesh.draw();
	}
}
