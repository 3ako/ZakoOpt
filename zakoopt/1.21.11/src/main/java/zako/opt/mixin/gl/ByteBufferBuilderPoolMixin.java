package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.raphimc.immediatelyfast.feature.core.ByteBufferBuilderPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.RingBuilders;

@Mixin(value = ByteBufferBuilderPool.class, remap = false)
public class ByteBufferBuilderPoolMixin {
	// ring-backed builders never came from the pool; they are released instead of pooled
	@Inject(method = "returnBufferBuilderSafe", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$release(ByteBufferBuilder builder, CallbackInfo ci) {
		if (((RingBuilders.Backed) builder).zakoopt$ringBacked()) {
			builder.close();
			ci.cancel();
		}
	}
}
