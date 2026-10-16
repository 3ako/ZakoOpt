package zako.opt;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
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
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (!overScene()) {
			super.extractBackground(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return !overScene();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		draw(new Canvas() {
			@Override
			public void fill(int x0, int y0, int x1, int y1, int argb) {
				graphics.fill(x0, y0, x1, y1, argb);
			}

			@Override
			public void text(Component text, int x, int y, int argb) {
				graphics.text(font, text, x, y, argb, false);
			}

			@Override
			public void text(FormattedCharSequence text, int x, int y, int argb) {
				graphics.text(font, text, x, y, argb, false);
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// 26.3 numbers buttons the SDL way, left is 1 there; Panel takes 0 as left
		return click(event.x(), event.y(), event.button() == InputConstants.MOUSE_BUTTON_LEFT ? 0 : -1) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		drag(event.x(), event.y());
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		release();
		return super.mouseReleased(event);
	}

	// the benchmark's screens stay up over the scene, and F3 is how to see what the frame costs
	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == GLFW_KEY_F3) {
			DebugToggle.toggle();
			return true;
		}
		if (event.isEscape() && escape() || event.key() == InputConstants.KEY_BACKSPACE && erase(event.hasControlDown())) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		return typed(event.codepoint()) || super.charTyped(event);
	}
}
