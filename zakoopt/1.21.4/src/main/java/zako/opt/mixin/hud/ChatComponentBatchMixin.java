package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.hud.ChatBatch;
import zako.opt.text.StableText;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatComponent.class)
public class ChatComponentBatchMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$begin(GuiGraphics graphics, int tick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
		ChatBatch.clear();
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
	private int zakoopt$deferLine(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, Operation<Integer> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(graphics, font, text, x, y, color);
		}
		ChatBatch.defer(graphics.pose().last().pose(), font, text, x, y, color);
		return 0;
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void zakoopt$flush(GuiGraphics graphics, int tick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
		ChatBatch.flush(graphics);
	}

	// a chat line keeps its text object for as long as it is shown, so the text mesh cache may hold on to it
	@ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
	private List<FormattedCharSequence> zakoopt$stableLines(List<FormattedCharSequence> lines) {
		if (!ZakoOptConfig.preparedTextCache()) {
			return lines;
		}
		List<FormattedCharSequence> stable = new ArrayList<>(lines.size());
		for (FormattedCharSequence line : lines) {
			stable.add(StableText.mark(line));
		}
		return stable;
	}
}
