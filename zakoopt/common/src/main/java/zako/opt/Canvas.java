package zako.opt;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public interface Canvas {
	void fill(int x0, int y0, int x1, int y1, int argb);

	void text(Component text, int x, int y, int argb);

	void text(FormattedCharSequence text, int x, int y, int argb);

	void clip(int x0, int y0, int x1, int y1);

	void unclip();
}
