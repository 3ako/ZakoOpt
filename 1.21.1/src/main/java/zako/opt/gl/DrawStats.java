package zako.opt.gl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

// per-frame draw counters for the F3 screen: counted only while it is open, shown as one-second averages
@UtilityClass
public class DrawStats {
	private final long WINDOW = 1_000_000_000L;
	private final int TOP = 5;
	private final Object2IntOpenHashMap<String> BY_LAYER = new Object2IntOpenHashMap<>();
	private boolean active;
	private int draws, layers, binds, frames;
	private long windowStart;
	private List<String> lines = List.of();

	public void draw() {
		if (active) {
			draws++;
		}
	}

	public void bind() {
		if (active) {
			binds++;
		}
	}

	public void layer(String name) {
		if (active) {
			layers++;
			BY_LAYER.addTo(name, 1);
		}
	}

	public void endFrame(boolean shown) {
		long now = System.nanoTime();
		if (!shown) {
			active = false;
			lines = List.of();
			return;
		}
		if (!active) {
			active = true;
			reset(now);
			return;
		}
		frames++;
		if (now - windowStart >= WINDOW) {
			float f = frames;
			String top = BY_LAYER.object2IntEntrySet().stream()
					.sorted((a, b) -> Integer.compare(b.getIntValue(), a.getIntValue())).limit(TOP)
					.map(e -> e.getKey() + " " + Math.round(e.getIntValue() / f)).collect(Collectors.joining(", "));
			lines = List.of(
					String.format("ZakoOpt per frame: %d draw calls, %d layer flushes (%d kinds), %d FBO binds",
							Math.round(draws / f), Math.round(layers / f), BY_LAYER.size(), Math.round(binds / f)),
					"Top layers: " + top);
			reset(now);
		}
	}

	public List<String> lines() {
		return lines;
	}

	private void reset(long now) {
		draws = layers = binds = frames = 0;
		BY_LAYER.clear();
		windowStart = now;
	}
}
