package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferAttachment;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.gl.SharedDepthFbos;

import java.util.List;

@Mixin(FrameBufferCache.class)
public class FrameBufferCacheMixin {
	@Shadow
	@Final
	private Object2IntMap<FrameBufferCache.CacheKey> cache;

	@Inject(method = "getFbo", at = @At("RETURN"))
	private void zakoopt$rememberDepth(DirectStateAccess dsa, List<FrameBufferAttachment> colors, FrameBufferAttachment depth, CallbackInfoReturnable<Integer> cir) {
		if (depth != null && depth.fboMipLevel() == 0) {
			SharedDepthFbos.remember(depth.glId(), cir.getReturnValueI());
		}
	}

	// runs before the FBO is deleted; its id may be reused by the next one
	@Inject(method = "destroyFbo", at = @At("HEAD"))
	private void zakoopt$forget(FrameBufferCache.CacheKey key, CallbackInfo ci) {
		if (cache.containsKey(key)) {
			SharedDepthFbos.forget(cache.getInt(key));
		}
	}
}
