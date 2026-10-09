package zako.opt.mixin.text;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;
import zako.opt.ZakoOptConfig;
import zako.opt.text.LruMap;
import zako.opt.text.StableText;

@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
	@Unique
	private final Map<Component, FormattedCharSequence> zakoopt$visualOrder = new LruMap<>(1024);
	// Component.hashCode walks the whole tree; most lookups are the same instance as last frame
	@Unique
	private final Map<Component, FormattedCharSequence> zakoopt$byIdentity = new IdentityHashMap<>();

	@Inject(method = "getVisualOrder", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cached(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
		if (!(text instanceof Component component) || !ZakoOptConfig.bidiCache()) {
			return;
		}
		boolean identity = ZakoOptConfig.microOpts2();
		FormattedCharSequence cached;
		synchronized (zakoopt$visualOrder) {
			cached = identity ? zakoopt$byIdentity.get(component) : null;
			if (cached == null) {
				cached = zakoopt$visualOrder.get(component);
				if (cached != null && identity) {
					zakoopt$remember(component, cached);
				}
			}
		}
		if (cached != null) {
			cir.setReturnValue(cached);
		}
	}

	@Inject(method = "getVisualOrder", at = @At("RETURN"), cancellable = true)
	private void zakoopt$store(FormattedText text, CallbackInfoReturnable<FormattedCharSequence> cir) {
		if (text instanceof Component component && ZakoOptConfig.bidiCache()) {
			FormattedCharSequence stable = StableText.mark(cir.getReturnValue());
			synchronized (zakoopt$visualOrder) {
				zakoopt$visualOrder.put(component, stable);
				if (ZakoOptConfig.microOpts2()) {
					zakoopt$remember(component, stable);
				}
			}
			cir.setReturnValue(stable);
		}
	}

	@Unique
	private void zakoopt$remember(Component component, FormattedCharSequence value) {
		// ponytail: wholesale clear instead of LRU, rebuilt from the equals map within a frame
		if (zakoopt$byIdentity.size() >= 2048) {
			zakoopt$byIdentity.clear();
		}
		zakoopt$byIdentity.put(component, value);
	}
}
