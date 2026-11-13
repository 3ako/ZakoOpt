package zako.opt.mixin.block;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

// features are rendered more than once a frame (particles pass): the sign text goes with the first, the main pass
@Mixin(LevelRenderer.class)
public class SignTextArmMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void zakoopt$arm(CallbackInfo ci) {
		SignTextMesh.arm();
	}
}
