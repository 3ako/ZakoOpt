package zako.opt.mixin.entity;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.ParallelModels;

// 1.21.1 renders entities inline in renderLevel: the "entities" profiler section opens the batch, and the first endLastBatch,
// which right after the entity loop flushes the entity buffers, closes it before anything gets drawn
@Mixin(LevelRenderer.class)
public class LevelRendererParallelMixin {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
			args = "ldc=entities", shift = At.Shift.AFTER))
	private void zakoopt$beginModels(CallbackInfo ci) {
		ParallelModels.begin();
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", ordinal = 0))
	private void zakoopt$endModels(CallbackInfo ci) {
		ParallelModels.end();
	}
}
