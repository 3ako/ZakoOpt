package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(HangingSignRenderer.class)
public abstract class HangingSignRendererMixin {
	@Unique
	private final Map<WoodType, Object> zakoopt$hangingModels = new IdentityHashMap<>();

	@WrapOperation(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object zakoopt$model(Map<WoodType, Object> models, Object wood, Operation<Object> original) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(models, wood);
		}
		return zakoopt$hangingModels.computeIfAbsent((WoodType) wood, w -> original.call(models, w));
	}
}
