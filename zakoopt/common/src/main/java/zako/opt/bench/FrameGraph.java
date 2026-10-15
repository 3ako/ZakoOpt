package zako.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import zako.opt.Canvas;
import zako.opt.Panel;

import java.util.Arrays;

// a column per frame, newest on the right, scaled to three medians so spikes stand out: yellow from 1.5 medians, red from 2.5
@UtilityClass
public class FrameGraph {
	private final long LABEL_NS = 500_000_000L;
	// the test measures FPS, so the panel over it draws without making garbage per frame
	private final long[] frames = new long[1024];
	private final long[] sorted = new long[1024];
	private long lastLabel;
	private Component label = Component.empty();

	public void draw(Canvas c, int x, int bottom, int width, int height) {
		c.fill(x, bottom - height, x + width, bottom, Panel.LINE);
		int n = Bench.lastFrames(frames, Math.min(width, frames.length));
		if (n == 0) {
			return;
		}
		System.arraycopy(frames, 0, sorted, 0, n);
		Arrays.sort(sorted, 0, n);
		double median = Math.max(sorted[n / 2], 1);
		for (int i = 0; i < n; i++) {
			double ratio = frames[i] / median;
			int h = (int) Math.max(1, Math.min(height, ratio / 3 * height));
			int column = x + width - n + i;
			c.fill(column, bottom - h, column + 1, bottom, ratio >= 2.5 ? Panel.BLOCKED : ratio >= 1.5 ? Panel.PIP : Panel.ACCENT);
		}
	}

	public Component label() {
		long now = System.nanoTime();
		if (now - lastLabel >= LABEL_NS) {
			lastLabel = now;
			double fps = Bench.recentFps();
			label = Component.literal(Math.round(fps) + " FPS · " + "%.1f".formatted(fps > 0 ? 1000 / fps : 0) + " ms");
		}
		return label;
	}
}
