package zako.opt.mixin.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import zako.opt.feature.Features;
import zako.opt.gl.GlCaches;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

// writeUniform maps the ring buffer, writes one block and unmaps it: a MappedView, a slice and a GpuBufferSlice per call.
// With a coherent persistent mapping the block can be written in place; only the slice the caller gets back is new. A buffer
// without a coherent mapping, or one the feature was not on for when it was created, keeps the vanilla path
@Mixin(DynamicUniformStorage.class)
public class GlUniformStorageMixin {

	@Inject(method = "writeUniform", at = @At("HEAD"), cancellable = true)
	private void zakoopt$writeOne(DynamicUniformStorage.DynamicUniform uniform, CallbackInfoReturnable<GpuBufferSlice> cir) {
		if (!Features.UNIFORM_WRITE.on) {
			return;
		}
		DynamicUniformStorageAccessor storage = (DynamicUniformStorageAccessor) this;
		int next = storage.zakoopt$nextBlock();
		int blockSize = storage.zakoopt$blockSize();
		DynamicUniformStorage.DynamicUniform last = storage.zakoopt$lastUniform();
		if (last != null && last.equals(uniform)) {
			// vanilla hands out the block it has just written instead of writing it again
			if (next > 0) {
				cir.setReturnValue(storage.zakoopt$ringBuffer().currentBuffer().slice((next - 1) * blockSize, blockSize));
			}
			return;
		}
		if (next >= storage.zakoopt$capacity()) {
			// a resize replaces the ring; let vanilla do it and write the block
			return;
		}
		GpuBuffer buffer = storage.zakoopt$ringBuffer().currentBuffer();
		if (!(buffer instanceof GlBuffer gl)) {
			return;
		}
		GlBufferAccessor accessor = (GlBufferAccessor) gl;
		if (!GlCaches.isCoherent(accessor.zakoopt$handle())) {
			return;
		}
		ByteBuffer persistent = accessor.zakoopt$persistentBuffer();
		if (persistent == null) {
			return;
		}
		int offset = next * blockSize;
		// a fresh view per write, like vanilla's map makes: a writer leaves the view at its end, so a kept one would start
		// the next write from there and run past the block
		uniform.write(MemoryUtil.memSlice(persistent, offset, blockSize));
		storage.zakoopt$setNextBlock(next + 1);
		storage.zakoopt$setLastUniform(uniform);
		cir.setReturnValue(buffer.slice(offset, blockSize));
	}

	@Inject(method = "writeUniforms", at = @At("HEAD"), cancellable = true)
	private void zakoopt$writeMany(DynamicUniformStorage.DynamicUniform[] uniforms, CallbackInfoReturnable<GpuBufferSlice[]> cir) {
		if (!Features.UNIFORM_WRITE.on || uniforms.length == 0) {
			return;
		}
		DynamicUniformStorageAccessor storage = (DynamicUniformStorageAccessor) this;
		int next = storage.zakoopt$nextBlock();
		int n = uniforms.length;
		if (next + n > storage.zakoopt$capacity()) {
			return;
		}
		GpuBuffer buffer = storage.zakoopt$ringBuffer().currentBuffer();
		if (!(buffer instanceof GlBuffer gl)) {
			return;
		}
		GlBufferAccessor accessor = (GlBufferAccessor) gl;
		if (!GlCaches.isCoherent(accessor.zakoopt$handle())) {
			return;
		}
		ByteBuffer persistent = accessor.zakoopt$persistentBuffer();
		if (persistent == null) {
			return;
		}
		int blockSize = storage.zakoopt$blockSize();
		GpuBufferSlice[] slices = new GpuBufferSlice[n];
		for (int i = 0; i < n; i++) {
			int block = next + i;
			slices[i] = buffer.slice(block * blockSize, blockSize);
			uniforms[i].write(MemoryUtil.memSlice(persistent, block * blockSize, blockSize));
		}
		storage.zakoopt$setNextBlock(next + n);
		storage.zakoopt$setLastUniform(uniforms[n - 1]);
		cir.setReturnValue(slices);
	}
}
