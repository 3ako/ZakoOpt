package zako.opt.mixin.block;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.SignWidths;

@Mixin(SignText.class)
public class SignTextWidthsMixin implements SignWidths {
	@Unique
	private final FormattedCharSequence[] zakoopt$measured = new FormattedCharSequence[4];
	@Unique
	private final int[] zakoopt$widths = new int[4];
	@Unique
	private int zakoopt$next;

	// by identity: the render messages are rebuilt as new objects whenever the text or the filtering changes
	@Override
	public int zakoopt$width(FormattedCharSequence text) {
		for (int i = 0; i < 4; i++) {
			if (zakoopt$measured[i] == text) {
				return zakoopt$widths[i];
			}
		}
		return -1;
	}

	@Override
	public void zakoopt$width(FormattedCharSequence text, int width) {
		zakoopt$measured[zakoopt$next] = text;
		zakoopt$widths[zakoopt$next] = width;
		zakoopt$next = zakoopt$next + 1 & 3;
	}
}
