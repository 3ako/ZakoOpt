package zako.opt.gl;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import lombok.experimental.UtilityClass;

// buffer attached to each texel-buffer texture; render thread only
@UtilityClass
public class TexBufferCache {
	private final Int2LongOpenHashMap ATTACHED = new Int2LongOpenHashMap();
	public int boundTexture;

	public boolean attached(int format, int buffer) {
		return ATTACHED.getOrDefault(boundTexture, -1L) == key(format, buffer);
	}

	public void attach(int format, int buffer) {
		ATTACHED.put(boundTexture, key(format, buffer));
	}

	public void forget() {
		ATTACHED.clear();
	}

	private long key(int format, int buffer) {
		return (long) format << 32 | (buffer & 0xFFFFFFFFL);
	}
}
