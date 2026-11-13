package zako.opt.mixin.block;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(FeatureRenderDispatcher.class)
public class SignTextDrawMixin {
	@Inject(method = "renderSolidFeatures", at = @At("TAIL"))
	private void zakoopt$drawSignText(CallbackInfo ci) {
		SignTextMesh.draw();
	}
}
