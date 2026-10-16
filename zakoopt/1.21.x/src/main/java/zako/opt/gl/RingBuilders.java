package zako.opt.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderType;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayDeque;

@UtilityClass
public class RingBuilders {
	private final int DEFAULT_SIZE = 64 << 10;
	// primitive values: the lookup ran per lease and boxed its default every time
	private final Reference2IntOpenHashMap<RenderType> SIZES = new Reference2IntOpenHashMap<>();
	// closed ring builders handed back on the render thread, attached again instead of making a new one per lease
	private final ArrayDeque<ByteBufferBuilder> FREE = new ArrayDeque<>();
	public RenderType current;

	public interface Backed {
		void zakoopt$attach(RenderType type, long address, int size);

		boolean zakoopt$ringBacked();
	}

	// null = take one from ImmediatelyFast's pool as usual
	public ByteBufferBuilder create() {
		RenderType type = current;
		// sorted types read their vertices back, and reads from this memory are slow
		if (type == null || type.sortOnUpload()) {
			return null;
		}
		int size = SIZES.getOrDefault(type, DEFAULT_SIZE);
		long address = ImmediateRing.reserveBuilder(type.format(), size);
		if (address == 0) {
			return null;
		}
		ByteBufferBuilder builder = FREE.isEmpty() ? new ByteBufferBuilder(16) : FREE.pop();
		((Backed) builder).zakoopt$attach(type, address, size);
		return builder;
	}

	// after close(), on the render thread
	public void recycle(ByteBufferBuilder builder) {
		if (FREE.size() < 256) {
			FREE.push(builder);
		}
	}

	public void remember(RenderType type, long size) {
		SIZES.put(type, (int) Math.min(size, 16 << 20));
	}
}
