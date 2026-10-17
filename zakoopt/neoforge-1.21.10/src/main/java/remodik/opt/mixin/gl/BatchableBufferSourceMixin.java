package remodik.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.raphimc.immediatelyfast.feature.core.BatchableBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import remodik.opt.gl.RingBuilders;

@Mixin(value = BatchableBufferSource.class, remap = false)
public class BatchableBufferSourceMixin {
	// getBuffer is an MC override; NeoForge runs on Mojang names in production too
	@Inject(method = "getBuffer", at = @At("HEAD"))
	private void zakoopt$renderType(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
		RingBuilders.current = type;
	}

	@WrapOperation(method = "getNextBufferAllocator", at = @At(value = "INVOKE", target = "Lnet/raphimc/immediatelyfast/feature/core/BufferAllocatorPool;borrowBufferAllocator()Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;"))
	private ByteBufferBuilder zakoopt$ringBuilder(Operation<ByteBufferBuilder> original) {
		ByteBufferBuilder builder = RingBuilders.create();
		return builder != null ? builder : original.call();
	}
}
