package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.TexBufferCache;

// only Direct deletes its GL handle; transient buffers are slices of a shared one
@Mixin(GlBuffer.Direct.class)
public class GlBufferCloseMixin {
	// a deleted buffer's handle can be reused by a new buffer, so the attachment cache must not outlive it
	@Inject(method = "close", at = @At("HEAD"))
	private void zakoopt$forget(CallbackInfo ci) {
		TexBufferCache.forget();
	}
}
