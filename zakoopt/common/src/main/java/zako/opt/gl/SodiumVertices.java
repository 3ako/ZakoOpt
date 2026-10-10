package zako.opt.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

// the only class touching Sodium's writer: loaded only when Sodium is installed
@UtilityClass
class SodiumVertices {
	boolean accepts(VertexConsumer consumer) {
		return VertexBufferWriter.tryOf(consumer) != null;
	}

	void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
		VertexBufferWriter.of(consumer).push(stack, src, count, format);
	}
}
