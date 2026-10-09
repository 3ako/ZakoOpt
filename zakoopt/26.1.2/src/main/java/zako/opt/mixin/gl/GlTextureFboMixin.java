package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.gl.DepthFbos;

@Mixin(GlTexture.class)
public class GlTextureFboMixin {
	@Inject(method = "getFbo", at = @At("RETURN"))
	private void zakoopt$rememberDepth(DirectStateAccess dsa, GpuTexture depth, CallbackInfoReturnable<Integer> cir) {
		if (depth instanceof GlTexture glDepth) {
			DepthFbos.remember((GlTexture) (Object) this, glDepth.glId(), cir.getReturnValue());
		}
	}
}
