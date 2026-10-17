package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.text.StableText;

import java.util.ArrayList;
import java.util.List;

// a text display keeps its split lines until its text or width changes: marked stable when they are split, holograms reach the
// text mesh cache like name tags do
@Mixin(Display.TextDisplay.class)
public class TextDisplayStableMixin {
	@WrapOperation(method = "cacheDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Display$TextDisplay$LineSplitter;split(Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;"))
	private Display.TextDisplay.CachedInfo zakoopt$stableLines(Display.TextDisplay.LineSplitter splitter, Component text, int width,
															   Operation<Display.TextDisplay.CachedInfo> original) {
		Display.TextDisplay.CachedInfo info = original.call(splitter, text, width);
		if (!Features.PREPARED_TEXT_CACHE.on) {
			return info;
		}
		List<Display.TextDisplay.CachedLine> lines = new ArrayList<>(info.lines().size());
		for (Display.TextDisplay.CachedLine line : info.lines()) {
			lines.add(new Display.TextDisplay.CachedLine(StableText.mark(line.contents()), line.width()));
		}
		return new Display.TextDisplay.CachedInfo(lines, info.width());
	}
}
