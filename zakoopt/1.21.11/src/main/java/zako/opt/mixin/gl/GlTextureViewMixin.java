package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.ZakoOptConfig;

@Mixin(GlTextureView.class)
public class GlTextureViewMixin {
	// render passes take the view's FBO, Sodium and clears the texture's: for a mip-0 view the attachments are identical,
	// yet two FBO objects meant a framebuffer switch at every hand-over between them
	@Inject(method = "getFbo", at = @At("HEAD"), cancellable = true)
	private void zakoopt$shareTextureFbo(DirectStateAccess dsa, GpuTexture depth, CallbackInfoReturnable<Integer> cir) {
		GlTextureView view = (GlTextureView) (Object) this;
		if (view.baseMipLevel() == 0 && ZakoOptConfig.fboShare()) {
			cir.setReturnValue(view.texture().getFbo(dsa, depth));
		}
	}
}
