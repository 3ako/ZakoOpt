package zako.opt.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.text.PreparedTextCache;

@Mixin(Font.class)
public class FontMixin {
	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cached(FormattedCharSequence text, float x, float y, int color, boolean shadow, int background,
								CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text)) {
			Font.PreparedText cached = PreparedTextCache.get(PreparedTextCache.key(text, x, y, color, shadow, false, background));
			if (cached != null) {
				cir.setReturnValue(cached);
			}
		}
	}

	@Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("RETURN"))
	private void zakoopt$store(FormattedCharSequence text, float x, float y, int color, boolean shadow, int background,
							   CallbackInfoReturnable<Font.PreparedText> cir) {
		if (PreparedTextCache.enabled() && PreparedTextCache.isStable(text)) {
			PreparedTextCache.Key key = PreparedTextCache.key(text, x, y, color, shadow, false, background);
			if (!PreparedTextCache.known(key)) {
				PreparedTextCache.put(key, cir.getReturnValue());
			}
		}
	}
}
