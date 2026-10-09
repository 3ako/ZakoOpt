package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import zako.opt.ZakoOptConfig;

@Mixin(RenderType.class)
public class RenderTypesMixin {
	@Unique
	private static final Map<ResourceLocation, RenderType> zakoopt$cutoutNoCull = new ConcurrentHashMap<>();
	@Unique
	private static final Map<ResourceLocation, RenderType> zakoopt$translucent = new ConcurrentHashMap<>();

	// Util.memoize keys on Pair(id, flag) and hashes it through Objects.hash on every call, per entity per frame
	@WrapOperation(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;",
			at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object zakoopt$cutoutNoCull(BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
		return zakoopt$lookup(zakoopt$cutoutNoCull, memo, id, flag, original);
	}

	@WrapOperation(method = "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;",
			at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static Object zakoopt$translucent(BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
		return zakoopt$lookup(zakoopt$translucent, memo, id, flag, original);
	}

	@Unique
	private static Object zakoopt$lookup(Map<ResourceLocation, RenderType> cache, BiFunction<Object, Object, Object> memo, Object id, Object flag, Operation<Object> original) {
		if (!(Boolean) flag || !ZakoOptConfig.lookupCaches()) {
			return original.call(memo, id, flag);
		}
		RenderType type = cache.get(id);
		if (type == null) {
			type = (RenderType) original.call(memo, id, flag);
			cache.put((ResourceLocation) id, type);
		}
		return type;
	}
}
