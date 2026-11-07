package zako.opt;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;

public interface Canvas {
	void fill(int x0, int y0, int x1, int y1, int argb);

	// rects packed as x0, y0, x1, y1, argb; a GUI fill is checked against every element added before it, so hundreds of
	// graph columns submitted one by one cost a quarter of the frame
	default void fills(int[] rects, int count) {
		for (int i = 0; i < count * 5; i += 5) {
			fill(rects[i], rects[i + 1], rects[i + 2], rects[i + 3], rects[i + 4]);
		}
	}

	static int rect(int[] rects, int at, int x0, int y0, int x1, int y1, int argb) {
		rects[at] = x0;
		rects[at + 1] = y0;
		rects[at + 2] = x1;
		rects[at + 3] = y1;
		rects[at + 4] = argb;
		return at + 5;
	}

	void text(Component text, int x, int y, int argb);

	void text(FormattedCharSequence text, int x, int y, int argb);

	void text(Component text, int x, int y, int argb, float scale);

	void item(Item item, int x, int y, float scale);

	void clip(int x0, int y0, int x1, int y1);

	void unclip();
}
