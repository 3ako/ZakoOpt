package zako.opt.text;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class PreparedTextCache {
	private final Font.PreparedText UNCACHEABLE = new Font.PreparedText() {
		@Override
		public void visit(Font.GlyphVisitor visitor) {
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenRectangle bounds() {
			return null;
		}
	};

	private final Map<Key, Font.PreparedText> CACHE = new LinkedHashMap<>(512, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Font.PreparedText> eldest) {
			return size() > 2048;
		}
	};

	public boolean isStable(FormattedCharSequence text) {
		return StableText.isStable(text);
	}

	public boolean enabled() {
		return ZakoOptConfig.preparedTextCache();
	}

	public Key key(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
		return new Key(text, x, y, color, shadow, includeEmpty, background);
	}

	public Font.PreparedText get(Key key) {
		synchronized (CACHE) {
			Font.PreparedText cached = CACHE.get(key);
			return cached == UNCACHEABLE ? null : cached;
		}
	}

	public boolean known(Key key) {
		synchronized (CACHE) {
			return CACHE.containsKey(key);
		}
	}

	public void put(Key key, Font.PreparedText prepared) {
		// obfuscated (§k) glyphs are re-randomized on every prepare, so they must not be frozen
		boolean plain = key.text.accept((index, style, codePoint) -> !style.isObfuscated());
		synchronized (CACHE) {
			CACHE.put(key, plain ? prepared : UNCACHEABLE);
		}
	}

	public void clear() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}

	public record Key(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.text == text && k.x == x && k.y == y && k.color == color
					&& k.shadow == shadow && k.includeEmpty == includeEmpty && k.background == background;
		}

		@Override
		public int hashCode() {
			int h = System.identityHashCode(text);
			h = 31 * h + Float.floatToIntBits(x);
			h = 31 * h + Float.floatToIntBits(y);
			h = 31 * h + color;
			h = 31 * h + background;
			return 31 * h + (shadow ? 1 : 0) + (includeEmpty ? 2 : 0);
		}
	}
}
