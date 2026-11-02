package zako.opt.mixin.block;

import zako.opt.feature.Features;

import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zako.opt.text.StableText;

import java.util.function.Function;

// a sign keeps its split lines until its text changes, so they may be marked stable and reach the text mesh cache
@Mixin(AbstractSignRenderer.class)
public class SignTextStableMixin {
	@ModifyArg(method = "renderSignText", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SignText;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;"), index = 1)
	private Function<Component, FormattedCharSequence> zakoopt$stableLines(Function<Component, FormattedCharSequence> split) {
		return Features.PREPARED_TEXT_CACHE.on ? component -> StableText.mark(split.apply(component)) : split;
	}
}
