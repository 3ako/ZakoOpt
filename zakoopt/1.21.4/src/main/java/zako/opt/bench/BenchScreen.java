package zako.opt.bench;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

// a screen over the running benchmark: no blur or dimming, the player has to see the scene through it; rects() are the
// panels behind its text, as {x0, y0, x1, y1, argb}
public abstract class BenchScreen extends Screen {
	protected BenchScreen(Component title) {
		super(title);
	}

	protected List<int[]> rects() {
		return List.of();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		for (int[] r : rects()) {
			graphics.fill(r[0], r[1], r[2], r[3], r[4]);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
