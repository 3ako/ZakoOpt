package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.ParallelModels;

// applied before the immediate ring's own resize/close hooks, which may move the memory themselves and cancel
@Mixin(value = ByteBufferBuilder.class, priority = 500)
public class ByteBufferBuilderMoveMixin {
	// parallel model workers write into reserved ranges by address; the memory must not move or go away under them
	@Inject(method = {"resize", "close"}, at = @At("HEAD"))
	private void zakoopt$waitForWriters(CallbackInfo ci) {
		ParallelModels.beforeMove();
	}
}
