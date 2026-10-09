package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.CachedLight;
import zako.opt.block.CachedValidity;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	// a HashSet lookup per block entity per frame; the answer only changes with the block state, so remember it per state
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean zakoopt$cachedValid(BlockEntityType<?> type, BlockState state, Operation<Boolean> original, BlockEntity blockEntity) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(type, state);
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		if (cache.zakoopt$validState() != state) {
			cache.zakoopt$setValid(state, original.call(type, state));
		}
		return cache.zakoopt$valid();
	}

	// distance check per block entity per frame; a tick late at the edge of view distance is invisible
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
	private boolean zakoopt$cachedDistance(BlockEntityRenderer<BlockEntity> renderer, BlockEntity blockEntity, Vec3 camera, Operation<Boolean> original) {
		if (!ZakoOptConfig.microOpts3() || blockEntity.getLevel() == null) {
			return original.call(renderer, blockEntity, camera);
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		long tick = blockEntity.getLevel().getGameTime();
		if (cache.zakoopt$distanceTick() != tick) {
			cache.zakoopt$setInDistance(original.call(renderer, blockEntity, camera), tick);
		}
		return cache.zakoopt$inDistance();
	}

	// light only changes on ticks; this ran per block entity per frame
	@WrapOperation(method = "setupAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
	private static int zakoopt$cachedLight(BlockAndTintGetter level, BlockPos pos, Operation<Integer> original,
										   BlockEntityRenderer<?> renderer, BlockEntity blockEntity, float partialTick, PoseStack pose, MultiBufferSource buffers, Vec3 camera) {
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
