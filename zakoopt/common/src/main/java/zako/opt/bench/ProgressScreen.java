package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

// what the benchmark is doing, how far along and how the frames go, in a panel at the left over the scene; Esc cancels
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProgressScreen extends BenchScreen {
	static final int WIDTH = 300;
	static final int GRAPH = 32;
	@NonFinal
	StringWidget stage;
	@NonFinal
	StringWidget fps;
	@NonFinal
	int x;
	@NonFinal
	int y;

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
	protected void init() {
		x = 8;
		y = height / 2 - 49;
		stage = line(y + 8, Component.empty());
		fps = line(y + 36 + GRAPH, Component.empty());
		line(y + 50 + GRAPH, hint().copy().withStyle(ChatFormatting.GRAY));
		tick();
	}

	// sized to its text, so it reads from the left edge in every version
	private StringWidget line(int lineY, Component text) {
		StringWidget widget = addRenderableWidget(new StringWidget(text, font));
		widget.setPosition(x + 8, lineY);
		return widget;
	}

	private void set(StringWidget widget, Component text) {
		widget.setMessage(text);
		widget.setWidth(Math.min(font.width(text), WIDTH - 16));
	}

	@Override
	public void tick() {
		set(stage, stage());
		set(fps, Component.literal(FrameGraph.fps()));
	}

	@Override
	protected List<int[]> rects() {
		int barX = x + 8, barY = y + 22, barW = WIDTH - 16;
		List<int[]> rects = new ArrayList<>();
		rects.add(new int[]{x, y, x + WIDTH, y + 66 + GRAPH, 0xB0000000});
		if (percent() >= 0) {
			rects.add(new int[]{barX, barY, barX + barW, barY + 5, 0xFF404040});
			rects.add(new int[]{barX, barY, barX + barW * Math.min(percent(), 100) / 100, barY + 5, 0xFF55FF55});
		}
		rects.addAll(FrameGraph.rects(barX, y + 30 + GRAPH, barW, GRAPH));
		return rects;
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
