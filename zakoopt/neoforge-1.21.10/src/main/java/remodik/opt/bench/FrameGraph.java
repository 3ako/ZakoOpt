package remodik.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import remodik.opt.Canvas;
import remodik.opt.Panel;

import java.util.Arrays;

// a column per frame, newest on the right, scaled to three medians so spikes stand out: orange from 1.5 medians, red from 2.5
@UtilityClass
public class FrameGraph {
	private final long LABEL_NS = 500_000_000L;
	public final int SMOOTH = Panel.ACCENT, STUTTER = 0xFFD08A1A, BIG_STUTTER = Panel.BLOCKED;
	// the test measures FPS, so the panel over it draws without making garbage per frame
	private final long[] frames = new long[1024];
	private final long[] sorted = new long[1024];
	private long lastLabel;
	private Component fps = Component.empty();
	private Component frame = Component.empty();

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
			c.fill(column, bottom - h, column + 1, bottom, ratio >= 2.5 ? BIG_STUTTER : ratio >= 1.5 ? STUTTER : SMOOTH);
		}
	}

	public Component fps() {
		long now = System.nanoTime();
		if (now - lastLabel >= LABEL_NS) {
			lastLabel = now;
			double recent = Bench.recentFps();
			long tenths = recent > 0 ? Math.round(10_000 / recent) : 0;
			fps = Component.translatable("zakoopt.bench.fps", Math.round(recent));
			frame = Component.translatable("zakoopt.bench.frame", tenths / 10, tenths % 10);
		}
		return fps;
	}

	// after fps(), which refreshes both
	public Component frame() {
		return frame;
	}
}
