package zako.opt.text;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.LinkedHashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class LruMap<K, V> extends LinkedHashMap<K, V> {
	int maxSize;

	public LruMap(int maxSize) {
		super(256, 0.75f, true);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}
}
