package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.entity.ParallelModels;
import zako.opt.gl.ReserveBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderReserveMixin implements ReserveBuffer {
	@Shadow
	private int vertices;
	@Shadow
	@Final
	private int vertexSize;
	@Shadow
	private long vertexPointer;
	@Shadow
	@Final
	private ByteBufferBuilder buffer;
	@Shadow
	private int elementsToFill;

	@Override
	public long zakoopt$reserve(int count) {
		int length = count * vertexSize;
		long dst = buffer.reserve(length);
		vertices += count;
		vertexPointer = dst + length - vertexSize;
		elementsToFill = 0;
		return dst - ((ByteBufferBuilderAccessor) buffer).zakoopt$pointer();
	}

	@Override
	public long zakoopt$address(long offset) {
		return ((ByteBufferBuilderAccessor) buffer).zakoopt$pointer() + offset;
	}

	// reserved vertices still being written elsewhere land before the mesh is taken
	@Inject(method = "build", at = @At("HEAD"))
	private void zakoopt$fillReserved(CallbackInfoReturnable<MeshData> cir) {
		ParallelModels.sync(this);
	}
}
