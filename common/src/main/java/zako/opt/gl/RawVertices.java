package zako.opt.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import zako.opt.Platform;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

// ready vertices straight into a buffer: through Sodium's VertexBufferWriter when Sodium is there, else into a vanilla BufferBuilder
@UtilityClass
public class RawVertices {
	private final boolean SODIUM = Platform.isModLoaded("sodium");
	private long scratch;
	private long scratchSize;

	public boolean accepts(VertexConsumer consumer, VertexFormat format) {
		// a BufferBuilder in another format (Iris extends the vanilla ones) needs elements the cached vertices lack
		if (consumer instanceof RawBuffer raw && !raw.zakoopt$accepts(format)) {
			return false;
		}
		return SODIUM ? SodiumVertices.accepts(consumer) : consumer instanceof RawBuffer;
	}

	// render thread only: valid until the next call, a whole mesh would overflow the MemoryStack
	public long scratch(long size) {
		if (size > scratchSize) {
			scratchSize = Math.max(size, scratchSize * 2);
			scratch = MemoryUtil.nmemRealloc(scratch, scratchSize);
		}
		return scratch;
	}

	public void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
		if (SODIUM) {
			SodiumVertices.push(consumer, stack, src, count, format);
		} else {
			((RawBuffer) consumer).zakoopt$push(src, count);
		}
	}
}
