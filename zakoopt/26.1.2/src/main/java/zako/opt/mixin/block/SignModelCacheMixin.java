package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.StandingSignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;

// the per-wood model sets live in a map keyed by WoodType records; the same keys come back every frame
@Mixin({StandingSignRenderer.class, HangingSignRenderer.class})
public class SignModelCacheMixin {
	@Unique
	private final Map<Object, Object> zakoopt$models = new IdentityHashMap<>();

	@WrapOperation(method = "getSignModel", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object zakoopt$model(Map<?, ?> map, Object key, Operation<Object> original) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(map, key);
		}
		return zakoopt$models.computeIfAbsent(key, k -> original.call(map, k));
	}
}
