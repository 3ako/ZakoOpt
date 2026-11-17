package zako.opt.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.text.PreparedTextCache;
import zako.opt.text.StableText;

@Mixin(Font.class)
public class FontMixin {
	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cached(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background,
								CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && StableText.isStable(text)) {
			Font.PreparedText cached = PreparedTextCache.get(new PreparedTextCache.Key(text, x, y, color, shadow, includeEmpty, background));
			if (cached != null) {
				cir.setReturnValue(cached);
			}
		}
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("RETURN"))
	private void zakoopt$store(FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background,
							   CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && StableText.isStable(text)) {
			PreparedTextCache.Key key = new PreparedTextCache.Key(text, x, y, color, shadow, includeEmpty, background);
			if (!PreparedTextCache.known(key)) {
				PreparedTextCache.put(key, cir.getReturnValue());
			}
		}
	}
}
