package zako.opt;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public abstract class PanelScreen extends Screen {
	private static final int GLFW_KEY_F3 = 292;

	protected PanelScreen(Component title) {
		super(title);
	}

	protected abstract void draw(Canvas canvas, int mouseX, int mouseY);

	protected abstract boolean click(double x, double y, int button);

	protected abstract void drag(double x, double y);

	protected abstract void release();

	protected boolean escape() {
		return false;
	}

	protected boolean erase(boolean all) {
		return false;
	}

	protected boolean typed(int codepoint) {
		return false;
	}

	// over the running benchmark: no blur or dimming, the player has to see the scene through it
	protected boolean overScene() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!overScene()) {
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return !overScene();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		draw(new Canvas() {
			@Override
			public void fill(int x0, int y0, int x1, int y1, int argb) {
				graphics.fill(x0, y0, x1, y1, argb);
			}

			@Override
			public void text(Component text, int x, int y, int argb) {
				graphics.drawString(font, text, x, y, argb, false);
			}

			@Override
			public void text(FormattedCharSequence text, int x, int y, int argb) {
				graphics.drawString(font, text, x, y, argb, false);
			}

			@Override
			public void text(Component text, int x, int y, int argb, float scale) {
				GuiPose.push(graphics, x, y, scale);
				graphics.drawString(font, text, 0, 0, argb, false);
				GuiPose.pop(graphics);
			}

			@Override
			public void clip(int x0, int y0, int x1, int y1) {
				graphics.enableScissor(x0, y0, x1, y1);
			}

			@Override
			public void unclip() {
				graphics.disableScissor();
			}
		}, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		return click(x, y, button) || super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		drag(x, y);
		return true;
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		release();
		return super.mouseReleased(x, y, button);
	}

	// the benchmark's screens stay up over the scene, and F3 is how to see what the frame costs
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW_KEY_F3) {
			DebugToggle.toggle();
			return true;
		}
		if (keyCode == InputConstants.KEY_ESCAPE && escape() || keyCode == InputConstants.KEY_BACKSPACE && erase(hasControlDown())) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return typed(codePoint) || super.charTyped(codePoint, modifiers);
	}
}
