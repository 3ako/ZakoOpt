package zako.opt.bench;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// a column per frame, newest on the right, scaled to three medians so spikes stand out: yellow from 1.5 medians, red from 2.5
@UtilityClass
public class FrameGraph {
	public List<int[]> rects(int x, int bottom, int width, int height) {
		List<int[]> rects = new ArrayList<>();
		rects.add(new int[]{x, bottom - height, x + width, bottom, 0x40FFFFFF});
		long[] frames = Bench.lastFrames(width);
		if (frames.length == 0) {
			return rects;
		}
		long[] sorted = frames.clone();
		Arrays.sort(sorted);
		double median = Math.max(sorted[sorted.length / 2], 1);
		for (int i = 0; i < frames.length; i++) {
			double ratio = frames[i] / median;
			int h = (int) Math.max(1, Math.min(height, ratio / 3 * height));
			int column = x + width - frames.length + i;
			rects.add(new int[]{column, bottom - h, column + 1, bottom, ratio >= 2.5 ? 0xFFFF5555 : ratio >= 1.5 ? 0xFFFFFF55 : 0xFF55FF55});
		}
		return rects;
	}

	public String fps() {
		double fps = Bench.recentFps();
		return Math.round(fps) + " FPS · " + "%.1f".formatted(fps > 0 ? 1000 / fps : 0) + " ms";
	}
}
