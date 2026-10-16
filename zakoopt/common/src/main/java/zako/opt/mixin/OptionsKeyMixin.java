package zako.opt.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoKey;

// the vanilla way in: the controls screen lists and saves what is in keyMappings, which load() reads the binding into
@Mixin(Options.class)
public class OptionsKeyMixin {
	@Mutable
	@Final
	@Shadow
	public KeyMapping[] keyMappings;

	@Inject(method = "load", at = @At("HEAD"))
	private void zakoopt$key(CallbackInfo ci) {
		if (!ArrayUtils.contains(keyMappings, ZakoKey.MONITOR)) {
			keyMappings = ArrayUtils.add(keyMappings, ZakoKey.MONITOR);
		}
	}
}
