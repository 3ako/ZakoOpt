package zako.opt.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderType;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayDeque;

@UtilityClass
public class RingBuilders {
	private final int DEFAULT_SIZE = 64 << 10;
	private final int MAX_SIZE = 16 << 20;
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
		// a type that outgrew the ring would grow out of it every frame, copying all it wrote back out of write-combined GPU
		// memory (17 FPS at a wall of factory gauges): it gets a plain builder instead
		if (size > MAX_SIZE) {
			return null;
		}
		long address = ImmediateRing.reserveBuilder(type.format(), size);
		if (address == 0) {
			return null;
		}
		ByteBufferBuilder builder = FREE.isEmpty() ? new ByteBufferBuilder(16) : FREE.pop();
		((Backed) builder).zakoopt$attach(type, address, size);
		return builder;
	}

	// true: a ring-backed builder, now closed and kept for the next lease when handed back on the render thread
	public boolean release(ByteBufferBuilder builder) {
		if (!((Backed) builder).zakoopt$ringBacked()) {
			return false;
		}
		builder.close();
		// a builder handed back twice must not be leased twice: two leases would write into the same native memory
		if (RenderSystem.isOnRenderThread() && FREE.size() < 256 && !FREE.contains(builder)) {
			FREE.push(builder);
		}
		return true;
	}

	public void remember(RenderType type, long size) {
		SIZES.put(type, (int) Math.min(size, Integer.MAX_VALUE));
	}
}
