package zako.opt.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

// the only class touching Sodium's writer: loaded only when Sodium is installed
@UtilityClass
class SodiumVertices {
	// tryOf is an interface instanceof plus an interface call, which thrashes HotSpot's per-class type check cache when a
	// handful of consumer classes alternate; most calls ask about the same buffer as the last one
	private Object lastWriter;

	boolean accepts(VertexConsumer consumer) {
		if (consumer == lastWriter) {
			return true;
		}
		if (VertexBufferWriter.tryOf(consumer) == null) {
			return false;
		}
		lastWriter = consumer;
		return true;
	}

	void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
		VertexBufferWriter writer = consumer == lastWriter ? (VertexBufferWriter) consumer : VertexBufferWriter.of(consumer);
		// copyInto mallocs the whole batch on the MemoryStack (64 KB by default): big meshes go in chunks, each in its own frame
		int stride = format.getVertexSize();
		int chunk = Math.max(1, 16384 / stride);
		for (int i = 0; i < count; i += chunk) {
			try (MemoryStack frame = stack.push()) {
				writer.push(frame, src + (long) i * stride, Math.min(chunk, count - i), format);
			}
		}
	}
}
