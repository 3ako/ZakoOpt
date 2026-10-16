package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class Panel extends PanelScreen {
	public static final int PANEL = 0xF5EFE6D2, FRAME = 0xFF8A7350, LINE = 0xFFD8CBB0, HOVER = 0xFFE6DCC5, SELECTED = 0xFFE2D5B8;
	public static final int TEXT = 0xFF2E2418, STRONG = 0xFF1A140C, MUTED = 0xFF7A6A52, DIM = 0xFFA0907A, BLOCKED = 0xFFB0502A, LINK = 0xFF2A6AA0;
	public static final int ACCENT = 0xFF3A8A3A, ON = 0xFF6AB86A, KNOB_ON = 0xFFFFFFFF, OFF = 0xFFC9BB9E, KNOB_OFF = 0xFF9A8A70, PIP = 0xFFC47A1A, VISUAL = 0xFF9A5A10, MARK = 0xFFF2C66B;
	public static final int MASTER_OFF = 0xFFD8CBB0, MASTER_OFF_FRAME = 0xFF8A7A60, BUTTON = 0xFFE2D5B8, BUTTON_FRAME = 0xFFA89070, BUTTON_HOVER = 0xFF5A4A30;
	protected static final int BUTTON_HEIGHT = 14;

	final List<Hit> hits = new ArrayList<>();
	@NonFinal
	protected int mx, my;
	Hit dragging;
	int clipTop, clipBottom;
	double scroll;
	int contentHeight, scrollTop, scrollBottom, scrollX1;

	private record Hit(int x0, int y0, int x1, int y1, Runnable click, ZakoOptions.Distance slider) {
		boolean contains(double x, double y) {
			return x >= x0 && x < x1 && y >= y0 && y < y1;
		}
	}

	protected Panel(Component title) {
		super(title);
	}

	protected abstract void paint(Canvas c);

	protected void changed() {
	}

	@Override
	protected final void draw(Canvas c, int mouseX, int mouseY) {
		hits.clear();
		clipTop = 0;
		clipBottom = height;
		mx = mouseX;
		my = mouseY;
		paint(c);
	}

	protected boolean over(int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1 && my >= clipTop && my < clipBottom;
	}

	protected void frame(Canvas c, int x0, int y0, int x1, int y1) {
		c.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, FRAME);
		c.fill(x0, y0, x1, y1, PANEL);
	}

	protected int lines(Canvas c, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			c.text(line, x, y, color);
			y += 10;
		}
		return y;
	}

	protected FormattedCharSequence firstLine(Component text, int width) {
		List<FormattedCharSequence> lines = font.split(text, width);
		return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.getFirst();
	}

	protected int lineCount(Component text, int width) {
		return font.split(text, width).size();
	}

	protected void toggle(Canvas c, int x, int y, boolean on, boolean blocked) {
		c.fill(x, y, x + 18, y + 8, on ? ON : OFF);
		int kx = on ? x + 11 : x + 1;
		c.fill(kx, y + 1, kx + 6, y + 7, blocked ? FRAME : on ? KNOB_ON : KNOB_OFF);
	}

	protected int buttonWidth(Component label) {
		return font.width(label) + 16;
	}

	protected boolean button(Canvas c, int x, int y, Component label, Runnable click) {
		return button(c, x, y, buttonWidth(label), label, BUTTON, BUTTON_FRAME, true, click);
	}

	protected boolean button(Canvas c, int x, int y, int w, Component label, int fill, int frame, boolean active, Runnable click) {
		boolean over = over(x, y, x + w, y + BUTTON_HEIGHT);
		c.fill(x, y, x + w, y + BUTTON_HEIGHT, active && over ? BUTTON_HOVER : frame);
		c.fill(x + 1, y + 1, x + w - 1, y + BUTTON_HEIGHT - 1, fill);
		c.text(label, x + (w - font.width(label)) / 2, y + 3, active ? TEXT : DIM);
		if (active) {
			hit(x, y, x + w, y + BUTTON_HEIGHT, click, null);
		}
		return over;
	}

	// true while the mouse is over the setting's row
	protected boolean setting(Canvas c, ZakoOptions.Option option, int x0, int x1, int y) {
		Component name = Component.translatable("zakoopt.option." + option.key());
		switch (option) {
			case ZakoOptions.Toggle t -> {
				toggle(c, x0, y + 1, t.get().get(), false);
				c.text(name, x0 + 24, y, TEXT);
				hit(x0, y - 2, x0 + 24 + font.width(name), y + 11, () -> t.set().accept(!t.get().get()), null);
			}
			case ZakoOptions.Distance d -> {
				int labelWidth = Math.min(font.width(name) + 12, (x1 - x0) / 2), valueWidth = 40;
				c.text(firstLine(name, labelWidth - 4), x0, y, TEXT);
				int tx0 = x0 + labelWidth, tx1 = x1 - valueWidth;
				int value = d.get().get();
				int kx = tx0 + (tx1 - tx0) * (value - d.min()) / Math.max(1, d.max() - d.min());
				c.fill(tx0, y + 3, tx1, y + 5, OFF);
				c.fill(tx0, y + 3, kx, y + 5, ACCENT);
				c.fill(kx - 2, y - 1, kx + 2, y + 9, ACCENT);
				c.fill(kx - 1, y, kx + 1, y + 8, dragging != null && dragging.slider() == d ? STRONG : KNOB_ON);
				Component text = Component.translatable(d.unit(), value);
				c.text(text, x1 - font.width(text), y, TEXT);
				hit(tx0 - 4, y - 3, tx1 + 4, y + 11, null, d);
			}
		}
		return over(x0, y - 2, x1, y + 12);
	}

	protected int beginScroll(Canvas c, int x0, int top, int x1, int bottom) {
		scrollTop = top;
		scrollBottom = bottom;
		scrollX1 = x1;
		scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - (bottom - top)));
		c.clip(x0, top, x1, bottom);
		clipTop = top;
		clipBottom = bottom;
		return top - (int) scroll;
	}

	protected void endScroll(Canvas c, int y) {
		contentHeight = y + (int) scroll - scrollTop;
		c.unclip();
		clipTop = 0;
		clipBottom = height;
		int view = scrollBottom - scrollTop;
		if (contentHeight > view) {
			int barHeight = Math.max(12, view * view / contentHeight);
			int barY = scrollTop + (int) ((view - barHeight) * scroll / (contentHeight - view));
			c.fill(scrollX1 - 3, barY, scrollX1 - 1, barY + barHeight, FRAME);
		}
	}

	protected void resetScroll() {
		scroll = 0;
	}

	protected void scrollTo(double y) {
		scroll = y;
	}

	protected void hit(int x0, int y0, int x1, int y1, Runnable click, ZakoOptions.Distance slider) {
		if (Math.max(y0, clipTop) < Math.min(y1, clipBottom)) {
			hits.add(new Hit(x0, Math.max(y0, clipTop), x1, Math.min(y1, clipBottom), click, slider));
		}
	}

	@Override
	protected boolean click(double x, double y, int button) {
		if (button != 0) {
			return false;
		}
		for (Hit hit : hits.reversed()) {
			if (hit.contains(x, y)) {
				if (hit.slider() != null) {
					dragging = hit;
					slide(x);
				} else {
					hit.click().run();
					changed();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	protected void drag(double x, double y) {
		if (dragging != null) {
			slide(x);
		}
	}

	@Override
	protected void release() {
		if (dragging != null) {
			dragging = null;
			changed();
		}
	}

	private void slide(double x) {
		ZakoOptions.Distance d = dragging.slider();
		double t = Mth.clamp((x - dragging.x0() - 4) / (dragging.x1() - dragging.x0() - 8), 0, 1);
		d.set().accept(d.min() + (int) Math.round(t * (d.max() - d.min())));
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll -= scrollY * 24;
		return true;
	}
}
