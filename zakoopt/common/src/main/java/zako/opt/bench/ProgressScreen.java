package zako.opt.bench;

import net.minecraft.network.chat.Component;
import zako.opt.Canvas;
import zako.opt.DebugToggle;
import zako.opt.Panel;

// what the benchmark is doing, how far along and how the frames go, in a panel at the left over the scene; Esc cancels
public class ProgressScreen extends Panel {
	static final int WIDTH = 300;
	static final int GRAPH = 32;

	public ProgressScreen() {
		super(Component.translatable("zakoopt.bench.title"));
	}

	protected Component stage() {
		return Bench.status().stage();
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
		// F3 sits where the panel is and shows the frame rate itself
		if (DebugToggle.visible()) {
			return;
		}
		// above the comparison panel at the bottom when the screen is short
		int h = 98 + 2 * GRAPH, w = Math.min(WIDTH, width - 16), x = 8, y = Math.max(8, Math.min(height / 2 - h / 2, height - 78 - h)), inner = w - 16;
		frame(c, x, y, x + w, y + h);
		c.text(firstLine(stage(), inner), x + 8, y + 8, STRONG);
		if (percent() >= 0) {
			c.fill(x + 8, y + 22, x + 8 + inner, y + 27, OFF);
			c.fill(x + 8, y + 22, x + 8 + inner * Math.min(percent(), 100) / 100, y + 27, ACCENT);
		}
		FrameGraph.draw(c, x + 8, y + 30 + GRAPH, inner, GRAPH);
		c.text(FrameGraph.label(), x + 8, y + 36 + GRAPH, TEXT);
		MemoryGraph.draw(c, x + 8, y + 50 + 2 * GRAPH, inner, GRAPH);
		c.text(MemoryGraph.label(), x + 8, y + 56 + 2 * GRAPH, TEXT);
		c.text(MemoryGraph.garbage(), x + 8, y + 68 + 2 * GRAPH, TEXT);
		c.text(firstLine(hint(), inner), x + 8, y + 82 + 2 * GRAPH, MUTED);
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
