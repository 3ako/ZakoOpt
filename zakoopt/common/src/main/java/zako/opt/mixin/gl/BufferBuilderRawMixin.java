package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import zako.opt.gl.RawBuffer;

// what Sodium's VertexBufferWriter does for a BufferBuilder, for running without Sodium
@Mixin(BufferBuilder.class)
public class BufferBuilderRawMixin implements RawBuffer {
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
	@Shadow
	@Final
	private VertexFormat format;

	@Override
	public boolean zakoopt$accepts(VertexFormat format) {
		return format == this.format;
	}

	@Override
	public void zakoopt$push(long src, int count) {
		int length = count * vertexSize;
		long dst = buffer.reserve(length);
		MemoryUtil.memCopy(src, dst, length);
		vertices += count;
		// the pushed vertices are complete: the next addVertex must not see a half-filled one
		vertexPointer = dst + length - vertexSize;
		elementsToFill = 0;
	}
}
