package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.gl.GpuTimers;

@Mixin(FrameGraphBuilder.class)
public class FrameGraphTimerMixin {
	@WrapOperation(method = "execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;beforeExecutePass(Ljava/lang/String;)V"))
	private void zakoopt$markPass(FrameGraphBuilder.Inspector inspector, String name, Operation<Void> original) {
		GpuTimers.mark(name);
		original.call(inspector, name);
	}
}
