package zako.opt.bench;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import zako.opt.Canvas;
import zako.opt.DebugToggle;
import zako.opt.Panel;
import zako.opt.ZakoOptConfig;

import java.lang.management.ManagementFactory;

// what the benchmark is doing, how far along and how the frames go, in a panel at the left over the scene; Esc cancels
public class ProgressScreen extends Panel {
	static final int WIDTH = 300;
	static final int FRAMES = 24, MEMORY = 16;
	static final int TRACK = 0xFFD2C4A6;
	static final float LEGEND = 0.75f;
	private static final long LOAD_NS = 500_000_000L;
	private static final int[] FRAME_COLORS = {FrameGraph.SMOOTH, FrameGraph.STUTTER, FrameGraph.BIG_STUTTER};
	private static final Component[] FRAME_LEGEND = {Component.translatable("zakoopt.bench.legend.smooth"),
			Component.translatable("zakoopt.bench.legend.stutter"), Component.translatable("zakoopt.bench.legend.big_stutter")};
	private static final int[] MEMORY_COLORS = {MemoryGraph.HEAP, MemoryGraph.GC, MemoryGraph.NATIVE};
	private static final Component[] MEMORY_LEGEND = {Component.translatable("zakoopt.bench.legend.heap"),
			Component.translatable("zakoopt.bench.legend.gc"), Component.translatable("zakoopt.bench.legend.native")};
	private static final Component CPU = Component.translatable("zakoopt.bench.cpu"), GPU = Component.translatable("zakoopt.bench.gpu");
	private static final Component MEMORY_TITLE = Component.translatable("zakoopt.bench.memory.title");
	private static final Component NONE = Component.literal("—");
	private static long lastLoad, painted, lastCpuNs;
	private static double cpu = -1, gpu = -1;
	private static Component cpuText = NONE, gpuText = NONE;

	public ProgressScreen() {
		super(Component.translatable("zakoopt.bench.title"));
	}

	// the GPU share is measured only while this panel is up; 26.3 moved the current screen, so the last paint tells;
	// a frame longer than this window is never counted
	public static boolean open() {
		return System.nanoTime() - painted < 2_000_000_000L;
	}

	protected Component stage() {
		return Bench.status().stage();
	}

	protected Component note() {
		return Bench.status().note();
	}

	// -1 hides the progress bar
	protected int percent() {
		return Bench.status().percent();
	}

	protected Component hint() {
		return Component.translatable("zakoopt.bench.hands");
	}

	@Override
	protected boolean overScene() {
		return true;
	}

	@Override
	protected void paint(Canvas c) {
		painted = System.nanoTime();
		// F3 sits where the panel is and shows the frame rate itself
		if (DebugToggle.visible()) {
			return;
		}
		load();
		int w = Math.min(WIDTH, width - 16), inner = w - 16;
		int frameRows = legendRows(FRAME_LEGEND, inner), memoryRows = legendRows(MEMORY_LEGEND, inner);
		int h = 113 + FRAMES + MEMORY + 8 * (frameRows + memoryRows);
		// above the comparison panel at the bottom when the screen is short
		int x = 8, y = Math.max(8, Math.min(height / 2 - h / 2, height - 78 - h)), l = x + 8;
		frame(c, x, y, x + w, y + h);

		Component note = note();
		int noteWidth = font.width(note);
		FormattedCharSequence stage = firstLine(stage(), noteWidth > 0 ? inner - noteWidth - 6 : inner);
		c.text(stage, l, y + 6, STRONG);
		if (noteWidth > 0) {
			c.text(note, l + font.width(stage) + 6, y + 6, MUTED);
		}
		if (percent() >= 0) {
			c.fill(l, y + 18, l + inner, y + 22, OFF);
			c.fill(l, y + 18, l + inner * Math.min(percent(), 100) / 100, y + 22, ACCENT);
		}

		Component fps = FrameGraph.fps();
		c.text(fps, l, y + 26, STRONG, 2);
		c.text(FrameGraph.frame(), l + 2 * font.width(fps) + 6, y + 33, MUTED);
		int row = y + 46 + FRAMES;
		FrameGraph.draw(c, l, row, inner, FRAMES);
		row = legend(c, l, row + 3, inner, FRAME_LEGEND, FRAME_COLORS) + 1;

		int half = (inner - 10) / 2;
		load(c, l, row, half, CPU, cpu, cpuText);
		load(c, l + inner - half, row, half, GPU, gpu, gpuText);
		row += 11;

		int titleWidth = font.width(MEMORY_TITLE);
		c.text(MEMORY_TITLE, l, row, MUTED);
		c.fill(l + titleWidth + 4, row + 4, l + inner, row + 5, LINE);
		row += 12 + MEMORY;
		MemoryGraph.draw(c, l, row, inner, MEMORY);
		row = legend(c, l, row + 3, inner, MEMORY_LEGEND, MEMORY_COLORS) + 1;
		c.text(MemoryGraph.label(), l, row, TEXT);
		c.text(MemoryGraph.garbage(), l, row + 10, MUTED);
		c.text(firstLine(hint(), inner), l, row + 21, DIM);
	}

	private static void load() {
		long now = System.nanoTime();
		if (now - lastLoad < LOAD_NS) {
			return;
		}
		// the render thread's share of one core: the frame waits on that thread, and a process average over every core reads 2% while it is full
		long cpuNs = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
		cpu = lastLoad == 0 || lastCpuNs <= 0 || cpuNs < 0 ? -1 : (cpuNs - lastCpuNs) * 100.0 / (now - lastLoad);
		lastCpuNs = cpuNs;
		lastLoad = now;
		gpu = GpuLoad.get();
		cpuText = percentText(cpu);
		gpuText = percentText(gpu);
	}

	private static Component percentText(double value) {
		return value < 0 ? NONE : Component.literal(Math.round(Math.min(value, 100)) + "%");
	}

	private void load(Canvas c, int x, int y, int width, Component name, double value, Component text) {
		int bar = x + font.width(name) + 4, end = x + width - font.width("100%") - 4;
		c.text(name, x, y, TEXT);
		c.fill(bar, y + 1, end, y + 6, TRACK);
		if (value > 0) {
			c.fill(bar, y + 1, bar + (int) ((end - bar) * Math.min(value, 100) / 100), y + 6, ACCENT);
		}
		c.text(text, x + width - font.width(text), y, TEXT);
	}

	private int legendRows(Component[] names, int width) {
		int rows = 1, at = 0;
		for (Component name : names) {
			int item = 6 + (int) Math.ceil(font.width(name) * LEGEND);
			if (at > 0 && at + item > width) {
				rows++;
				at = 0;
			}
			at += item + 6;
		}
		return rows;
	}

	// a colour square and a name per item, wrapped to the width; the y under the last row
	private int legend(Canvas c, int x, int y, int width, Component[] names, int[] colors) {
		int at = 0;
		for (int i = 0; i < names.length; i++) {
			int item = 6 + (int) Math.ceil(font.width(names[i]) * LEGEND);
			if (at > 0 && at + item > width) {
				y += 8;
				at = 0;
			}
			c.fill(x + at, y + 1, x + at + 4, y + 5, colors[i]);
			c.text(names[i], x + at + 6, y, MUTED, LEGEND);
			at += item + 6;
		}
		return y + 8;
	}

	protected void modToggle(Canvas c, int x, int y, int width) {
		boolean on = ZakoOptConfig.enabled;
		Component toggle = Component.translatable(on ? "zakoopt.bench.compare.toggle.on" : "zakoopt.bench.compare.toggle.off");
		button(c, x, y, width, toggle, on ? ON : MASTER_OFF, on ? ACCENT : MASTER_OFF_FRAME, true, () -> ZakoOptConfig.enabled = !ZakoOptConfig.enabled);
	}

	protected void closed() {
		Bench.cancel();
	}

	@Override
	public void onClose() {
		closed();
		super.onClose();
	}
}
