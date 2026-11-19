package zako.opt.mixin.gl;

import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

// a persistent mapping without GL_MAP_COHERENT_BIT has to be flushed after every write; a coherent one reaches the GPU
// by itself. Buffers created before the switch stay non-coherent and keep their flushes
@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage$Immutable")
public class BufferStorageCoherentMixin {
	@WrapOperation(
		method = "tryMapBufferPersistent",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/DirectStateAccess;mapBufferRange(IJJII)Ljava/nio/ByteBuffer;")
	)
	private ByteBuffer zakoopt$coherent(DirectStateAccess dsa, int handle, long offset, long length, int flags, int usage, Operation<ByteBuffer> original) {
		if (Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.coherentBuffers && (flags & 2) != 0) {
			flags |= 128;
		}
		return original.call(dsa, handle, offset, length, flags, usage);
	}

	@Inject(method = "tryMapBufferPersistent", at = @At("RETURN"))
	private void zakoopt$mark(DirectStateAccess dsa, int usage, int handle, long size, CallbackInfoReturnable<ByteBuffer> cir) {
		if (cir.getReturnValue() != null && (usage & 2) != 0 && Features.DRIVER_CALLS.on && Features.DRIVER_CALLS.coherentBuffers) {
			GlCaches.markCoherent(handle);
		}
	}
}
