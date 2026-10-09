package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.DepthFbos;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderDepthClearMixin {
	@Inject(method = "clearDepthTexture", at = @At("HEAD"), cancellable = true)
	private void zakoopt$clearInPlace(GpuTexture depth, double value, CallbackInfo ci) {
		if (ZakoOptConfig.fboShare() && depth instanceof GlTexture gl && !gl.isClosed() && DepthFbos.clear(gl.glId(), value)) {
			ci.cancel();
		}
	}
}
