package zako.opt.mixin.block;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.SignTextMesh;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public class SignTextDrawMixin {
	@Inject(method = "executeTranslucent", at = @At("TAIL"))
	private void zakoopt$drawSignText(RenderPass pass, CallbackInfo ci) {
		SignTextMesh.draw(pass);
	}
}
