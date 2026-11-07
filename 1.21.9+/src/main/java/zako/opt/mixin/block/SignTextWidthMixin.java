package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.block.SignWidths;

// a sign's lines were measured glyph by glyph every frame to center them, though they only change with the text
@Mixin(AbstractSignRenderer.class)
public class SignTextWidthMixin {
	@WrapOperation(method = "submitSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"))
	private int zakoopt$cachedWidth(Font font, FormattedCharSequence text, Operation<Integer> original, @Local SignText signText) {
		if (!Features.LOOKUP_CACHES.on) {
			return original.call(font, text);
		}
		SignWidths widths = (SignWidths) signText;
		int width = widths.zakoopt$width(text);
		if (width < 0) {
			width = original.call(font, text);
			widths.zakoopt$width(text, width);
		}
		return width;
	}
}
