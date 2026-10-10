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
		(consumer == lastWriter ? (VertexBufferWriter) consumer : VertexBufferWriter.of(consumer)).push(stack, src, count, format);
	}
}
