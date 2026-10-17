package remodik.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import remodik.opt.feature.Feature;
import remodik.opt.feature.Features;

// why an option is held off on this setup whatever its setting, worded for the options screens; null: it works as set
@UtilityClass
public class Blocked {
	public Component reason(String key) {
		Feature feature = Features.byKey(key);
		String cause = feature == null ? null : feature.blocked();
		return cause == null ? null : Component.translatable("zakoopt.blocked",
				cause.startsWith("zakoopt.") ? Component.translatable(cause) : Component.literal(cause));
	}
}
