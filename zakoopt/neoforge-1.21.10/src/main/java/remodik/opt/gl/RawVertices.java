package remodik.opt.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryStack;

// ready vertices straight into a buffer: through Sodium's VertexBufferWriter when Sodium is there, else into a vanilla BufferBuilder
@UtilityClass
public class RawVertices {
	private final boolean SODIUM = remodik.opt.Platform.isModLoaded("sodium");

	public boolean accepts(VertexConsumer consumer, VertexFormat format) {
		// a BufferBuilder in another format (Iris extends the vanilla ones) needs elements the cached vertices lack
		if (consumer instanceof RawBuffer raw && !raw.zakoopt$accepts(format)) {
			return false;
		}
		return SODIUM ? SodiumVertices.accepts(consumer) : consumer instanceof RawBuffer;
	}

	public void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
		if (SODIUM) {
			SodiumVertices.push(consumer, stack, src, count, format);
		} else {
			((RawBuffer) consumer).zakoopt$push(src, count);
		}
	}
}
