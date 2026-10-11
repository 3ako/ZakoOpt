package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.CachedLight;

@Mixin(BlockEntityRenderState.class)
public class BlockEntityRenderStateMixin {
	// light only changes on ticks; this ran per block entity per frame
	@WrapOperation(method = "extractBase", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/LightCoordsUtil;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
	private static int zakoopt$cachedLight(BlockAndLightGetter level, BlockPos pos, Operation<Integer> original,
										   BlockEntity blockEntity, BlockEntityRenderState state, ModelFeatureRenderer.CrumblingOverlay crumbling) {
		if (!ZakoOptConfig.lookupCaches() || !(level instanceof Level world)) {
			return original.call(level, pos);
		}
		CachedLight cache = (CachedLight) blockEntity;
		long tick = world.getGameTime();
		if (cache.zakoopt$lightTick() != tick) {
			cache.zakoopt$setLight(original.call(level, pos), tick);
		}
		return cache.zakoopt$light();
	}
}
