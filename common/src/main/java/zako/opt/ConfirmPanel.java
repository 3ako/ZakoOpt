package zako.opt;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.chat.Component;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfirmPanel extends Panel {
	Component text, yes, no;
	BooleanConsumer answer;

	public ConfirmPanel(BooleanConsumer answer, Component title, Component text, Component yes, Component no) {
		super(title);
		this.text = text;
		this.yes = yes;
		this.no = no;
		this.answer = answer;
	}

	@Override
	protected void paint(Canvas c) {
		int w = Math.min(width - 12, 360), x0 = (width - w) / 2, x1 = x0 + w, inner = w - 20;
		int h = Math.min(height - 12, 24 + lineCount(text, inner) * 10 + 8 + BUTTON_HEIGHT + 10), y0 = (height - h) / 2, y1 = y0 + h;
		frame(c, x0, y0, x1, y1);
		c.text(title, x0 + 10, y0 + 8, STRONG);
		int bottom = y1 - BUTTON_HEIGHT - 16;
		endScroll(c, lines(c, text, x0 + 10, beginScroll(c, x0, y0 + 22, x1, bottom) + 2, inner, TEXT));
		int nw = buttonWidth(no), by = y1 - BUTTON_HEIGHT - 8;
		button(c, x1 - 8 - nw, by, no, () -> answer.accept(false));
		int yw = buttonWidth(yes);
		button(c, x1 - 8 - nw - 6 - yw, by, yw, yes, ON, ACCENT, true, () -> answer.accept(true));
	}

	@Override
	public void onClose() {
		answer.accept(false);
	}
}
