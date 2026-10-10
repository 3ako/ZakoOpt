package zako.opt.mixin.text;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(EntityRenderer.class)
public class EntityRendererNameTagMixin {
	// a tag's component is the same instance for a whole tick; width and getString both walk the whole text every frame
	@Unique
	private static final Map<Component, Integer> zakoopt$widths = new IdentityHashMap<>();
	@Unique
	private static final Map<Component, String> zakoopt$strings = new IdentityHashMap<>();

	@WrapOperation(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"))
	private int zakoopt$cachedWidth(Font font, FormattedText text, Operation<Integer> original) {
		if (!(text instanceof Component component) || !ZakoOptConfig.preparedTextCache()) {
			return original.call(font, text);
		}
		Integer width = zakoopt$widths.get(component);
		if (width == null) {
			// ponytail: wholesale clear instead of LRU, the live tags refill it within a frame
			if (zakoopt$widths.size() >= 2048) {
				zakoopt$widths.clear();
			}
			width = original.call(font, text);
			zakoopt$widths.put(component, width);
		}
		return width;
	}

	@WrapOperation(method = "renderNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
	private String zakoopt$cachedString(Component component, Operation<String> original) {
		if (!ZakoOptConfig.preparedTextCache()) {
			return original.call(component);
		}
		String string = zakoopt$strings.get(component);
		if (string == null) {
			if (zakoopt$strings.size() >= 2048) {
				zakoopt$strings.clear();
			}
			string = original.call(component);
			zakoopt$strings.put(component, string);
		}
		return string;
	}
}
