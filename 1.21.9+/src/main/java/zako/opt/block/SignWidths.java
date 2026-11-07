package zako.opt.block;

import net.minecraft.util.FormattedCharSequence;

public interface SignWidths {
	int zakoopt$width(FormattedCharSequence text);

	void zakoopt$width(FormattedCharSequence text, int width);
}
