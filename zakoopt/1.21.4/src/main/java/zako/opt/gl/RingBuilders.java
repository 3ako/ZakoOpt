package zako.opt.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderType;

import java.util.IdentityHashMap;
import java.util.Map;

@UtilityClass
public class RingBuilders {
	private final int DEFAULT_SIZE = 64 << 10;
	private final Map<RenderType, Integer> SIZES = new IdentityHashMap<>();
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
		ByteBufferBuilder builder = new ByteBufferBuilder(16);
		((Backed) builder).zakoopt$attach(type, address, size);
		return builder;
	}

	public void remember(RenderType type, long size) {
		SIZES.put(type, (int) Math.min(size, 16 << 20));
	}
}
