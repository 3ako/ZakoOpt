package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.box.PackageItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.feature.Features;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Pseudo
@Mixin(value = PackageItem.class, remap = false)
public class CreatePackageAddressMixin {
	// chain conveyors route every package past every exit each tick, and String.matches compiles the address glob's regex anew each time
	@Unique
	private static final Map<String, Pattern> zakoopt$patterns = new ConcurrentHashMap<>();

	@WrapOperation(require = 0, method = "matchAddress(Ljava/lang/String;Ljava/lang/String;)Z",
			at = @At(value = "INVOKE", target = "Ljava/lang/String;matches(Ljava/lang/String;)Z"))
	private static boolean zakoopt$cachedPattern(String text, String regex, Operation<Boolean> original) {
		if (!Features.CREATE_ADDRESSES.on) {
			return original.call(text, regex);
		}
		// ponytail: wholesale clear instead of LRU, a base has far fewer addresses than this
		if (zakoopt$patterns.size() >= 4096) {
			zakoopt$patterns.clear();
		}
		return zakoopt$patterns.computeIfAbsent(regex, Pattern::compile).matcher(text).matches();
	}
}
