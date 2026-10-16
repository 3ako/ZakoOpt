package zako.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import zako.opt.Canvas;
import zako.opt.Panel;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;

// heap and native memory during the test, a column per quarter second, newest on the right: heap in use as a filled saw
// that drops at every GC, the committed heap and native memory as lines, a GC as a faint full-height tick
@UtilityClass
public class MemoryGraph {
	// the process size is a system call: no more than four samples a second
	private final long SAMPLE_NS = 250_000_000L;
	private final long LABEL_NS = 500_000_000L;
	private final int HISTORY = 512;
	public final int HEAP = Panel.LINK, COMMITTED = Panel.FRAME, NATIVE = Panel.PIP, GC = 0x40000000;
	private final float[] used = new float[HISTORY];
	private final float[] committed = new float[HISTORY];
	private final float[] process = new float[HISTORY];
	private final boolean[] gc = new boolean[HISTORY];
	private final List<GarbageCollectorMXBean> GCS = ManagementFactory.getGarbageCollectorMXBeans();
	private final com.sun.management.OperatingSystemMXBean OS =
			ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os ? os : null;
	private int samples;
	private long lastSample;
	private long lastLabel;
	private long gcCount = -1;
	private long allocated = -1;
	private long renderAllocated = -1;
	private int labelFrames;
	private final com.sun.management.ThreadMXBean THREADS =
			ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t ? t : null;
	private Component label = Component.empty();
	private Component garbage = Component.empty();

	public void reset() {
		samples = 0;
		lastSample = 0;
		gcCount = -1;
		allocated = -1;
		renderAllocated = -1;
		labelFrames = 0;
		label = Component.empty();
		garbage = Component.empty();
	}

	// every thread of the process since the JVM started, ended ones included: -1 where the JVM cannot tell
	private long allocatedBytes() {
		try {
			return THREADS == null ? -1 : THREADS.getTotalThreadAllocatedBytes();
		} catch (UnsupportedOperationException e) {
			return -1;
		}
	}

	// every frame of the test; values in MB, Runtime because MemoryMXBean makes a MemoryUsage per call
	public void frame(long now) {
		labelFrames++;
		if (now - lastSample < SAMPLE_NS) {
			return;
		}
		lastSample = now;
		Runtime rt = Runtime.getRuntime();
		long count = 0;
		for (GarbageCollectorMXBean bean : GCS) {
			count += Math.max(0, bean.getCollectionCount());
		}
		int i = samples & HISTORY - 1;
		used[i] = (rt.totalMemory() - rt.freeMemory()) / 1048576f;
		committed[i] = rt.totalMemory() / 1048576f;
		// private bytes on Windows: heap, metaspace, JIT code, threads, the GL driver and LWJGL buffers together
		process[i] = OS == null ? 0 : OS.getCommittedVirtualMemorySize() / 1048576f;
		gc[i] = gcCount >= 0 && count != gcCount;
		gcCount = count;
		samples++;
		if (now - lastLabel >= LABEL_NS) {
			long bytes = allocatedBytes();
			// garbage made per second: with the mod on and off the difference shows as a number, not only as a steeper saw
			String rate = allocated < 0 || bytes < 0 ? "?" : String.valueOf(Math.round((bytes - allocated) / 1048576.0 / ((now - lastLabel) / 1e9)));
			allocated = bytes;
			// the render thread's own garbage per frame: per second the mod on and off differ little, per frame several times
			long render = THREADS == null ? -1 : THREADS.getCurrentThreadAllocatedBytes();
			String perFrame = renderAllocated < 0 || render < 0 || labelFrames == 0 ? "?" : String.valueOf(Math.round((render - renderAllocated) / 1024.0 / labelFrames));
			renderAllocated = render;
			labelFrames = 0;
			lastLabel = now;
			label = Component.translatable("zakoopt.bench.memory",
					Component.literal(String.valueOf((int) used[i])).withColor(HEAP), String.valueOf(rt.maxMemory() >> 20),
					OS == null ? Component.literal("?") : size(Math.max(0, process[i] - committed[i])).withColor(NATIVE));
			garbage = Component.translatable("zakoopt.bench.garbage", rate, perFrame);
		}
	}

	public Component label() {
		return label;
	}

	public Component garbage() {
		return garbage;
	}

	public void draw(Canvas c, int x, int bottom, int width, int height) {
		c.fill(x, bottom - height, x + width, bottom, Panel.LINE);
		int n = Math.min(samples, Math.min(width, HISTORY));
		float scale = 1;
		for (int k = samples - n; k < samples; k++) {
			int i = k & HISTORY - 1;
			scale = Math.max(scale, Math.max(committed[i], process[i] - committed[i]));
		}
		for (int k = samples - n; k < samples; k++) {
			int i = k & HISTORY - 1, cx = x + width - (samples - k);
			if (gc[i]) {
				c.fill(cx, bottom - height, cx + 1, bottom, GC);
			}
			c.fill(cx, y(bottom, height, used[i], scale), cx + 1, bottom, HEAP);
			int cy = y(bottom, height, committed[i], scale);
			c.fill(cx, cy, cx + 1, cy + 1, COMMITTED);
			if (OS != null) {
				int ny = y(bottom, height, process[i] - committed[i], scale);
				c.fill(cx, ny, cx + 1, ny + 1, NATIVE);
			}
		}
	}

	private int y(int bottom, int height, float mb, float scale) {
		return bottom - Math.max(1, Math.min(height, Math.round(mb / scale * height)));
	}

	private MutableComponent size(float mb) {
		return mb < 1024 ? Component.translatable("zakoopt.bench.mb", (int) mb)
				: Component.translatable("zakoopt.bench.gb", String.format(Locale.ROOT, "%.1f", mb / 1024));
	}
}
