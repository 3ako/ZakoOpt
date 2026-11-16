package zako.opt.gl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// the only class touching Sodium's writer: loaded only when Sodium is installed
@UtilityClass
class SodiumVertices {
	// Sodium 0.9.3-beta.5 prefixed the writer's methods with sodium$; both names are looked up
	private final MethodHandle TRY_OF = find(true, "tryOf", MethodType.methodType(VertexBufferWriter.class, VertexConsumer.class));
	private final MethodHandle PUSH = find(false, "push", MethodType.methodType(void.class, MemoryStack.class, long.class, int.class, VertexFormat.class));
	// tryOf is an interface instanceof plus an interface call, which thrashes HotSpot's per-class type check cache when a
	// handful of consumer classes alternate; most calls ask about the same buffer as the last one
	private Object lastWriter;

	@SneakyThrows
	private MethodHandle find(boolean isStatic, String name, MethodType type) {
		MethodHandles.Lookup lookup = MethodHandles.publicLookup();
		for (String candidate : new String[]{"sodium$" + name, name}) {
			try {
				return isStatic ? lookup.findStatic(VertexBufferWriter.class, candidate, type) : lookup.findVirtual(VertexBufferWriter.class, candidate, type);
			} catch (NoSuchMethodException ignored) {
			}
		}
		throw new NoSuchMethodException("VertexBufferWriter." + name);
	}

	@SneakyThrows
	boolean accepts(VertexConsumer consumer) {
		if (consumer == lastWriter) {
			return true;
		}
		if ((VertexBufferWriter) TRY_OF.invokeExact(consumer) == null) {
			return false;
		}
		lastWriter = consumer;
		return true;
	}

	@SneakyThrows
	void push(VertexConsumer consumer, MemoryStack stack, long src, int count, VertexFormat format) {
		VertexBufferWriter writer = consumer == lastWriter ? (VertexBufferWriter) consumer : (VertexBufferWriter) TRY_OF.invokeExact(consumer);
		// copyInto mallocs the whole batch on the MemoryStack (64 KB by default): big meshes go in chunks, each in its own frame
		int stride = format.getVertexSize();
		int chunk = Math.max(1, 16384 / stride);
		for (int i = 0; i < count; i += chunk) {
			try (MemoryStack frame = stack.push()) {
				PUSH.invokeExact(writer, frame, src + (long) i * stride, Math.min(chunk, count - i), format);
			}
		}
	}
}
