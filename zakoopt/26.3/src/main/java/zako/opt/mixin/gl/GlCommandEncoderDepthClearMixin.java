package zako.opt.mixin.gl;

import com.mojang.renderpearl.backend.opengl.GlTexture;
import com.mojang.renderpearl.api.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.SharedDepthFbos;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public class GlCommandEncoderDepthClearMixin {
	// vanilla attaches the depth texture to a scratch FBO, clears, detaches and unbinds; a cached FBO already holds it
	@Inject(method = "clearDepthTexture", at = @At("HEAD"), cancellable = true)
	private void zakoopt$clearInPlace(GpuTexture depth, double value, CallbackInfo ci) {
		if (ZakoOptConfig.fboShare() && depth instanceof GlTexture gl && !gl.isClosed() && SharedDepthFbos.clear(gl.glId(), value)) {
			ci.cancel();
		}
	}
}
