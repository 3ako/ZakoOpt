package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.block.BlockEntityDistance;
import zako.opt.block.CachedValidity;
import zako.opt.entity.PooledState;
import zako.opt.feature.Features;

@Mixin(BlockEntityRenderDispatcher.class)
public class NeoBlockEntityRenderDispatcherMixin {
	// 26.2+ passes a boolean before the Frustum, 26.1.2 does not
	private static final String METHOD = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;ZLnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;";
	private static final String METHOD_26_1 = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;";

	@Shadow
	private Vec3 cameraPos;

	@Inject(method = {METHOD, METHOD_26_1}, at = @At("HEAD"), cancellable = true)
	private void zakoopt$earlyCull(CallbackInfoReturnable<BlockEntityRenderState> cir, @Local(argsOnly = true) BlockEntity blockEntity) {
		if (BlockEntityDistance.culledEarly((BlockEntityRenderDispatcher) (Object) this, blockEntity, cameraPos)) {
			cir.setReturnValue(null);
		}
	}

	@WrapOperation(method = {METHOD, METHOD_26_1}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean zakoopt$cachedValid(BlockEntityType<?> type, BlockState state, Operation<Boolean> original, @Local(argsOnly = true) BlockEntity blockEntity) {
		if (!Features.SIGN_CACHE.on) {
			return original.call(type, state);
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		if (cache.zakoopt$validState() != state) {
			cache.zakoopt$setValid(state, original.call(type, state));
		}
		return cache.zakoopt$valid();
	}

	@WrapOperation(method = {METHOD, METHOD_26_1}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/phys/Vec3;)Z"))
	private boolean zakoopt$cachedDistance(BlockEntityRenderer<BlockEntity, ?> renderer, BlockEntity blockEntity, Vec3 camera, Operation<Boolean> original) {
		if (!Features.MICRO_OPTS.fewerChecks || blockEntity.getLevel() == null) {
			return original.call(renderer, blockEntity, camera);
		}
		CachedValidity cache = (CachedValidity) blockEntity;
		long tick = blockEntity.getLevel().getGameTime();
		if (cache.zakoopt$distanceTick() != tick) {
			cache.zakoopt$setInDistance(original.call(renderer, blockEntity, camera), tick);
		}
		return cache.zakoopt$inDistance();
	}

	@WrapOperation(method = {METHOD, METHOD_26_1}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"))
	private BlockEntityRenderState zakoopt$pooled(BlockEntityRenderer<?, ?> renderer, Operation<BlockEntityRenderState> original,
												  @Local(argsOnly = true) BlockEntity blockEntity) {
		Class<?> type = renderer.getClass();
		if (!Features.FEWER_ALLOCATIONS.on || type != ChestRenderer.class && type != SpawnerRenderer.class && type != PistonHeadRenderer.class) {
			return original.call(renderer);
		}
		PooledState pooled = (PooledState) blockEntity;
		if (pooled.zakoopt$stateOwner() == renderer) {
			BlockEntityRenderState state = (BlockEntityRenderState) pooled.zakoopt$state();
			if (state instanceof SpawnerRenderState spawner) {
				spawner.displayEntity = null;
			}
			return state;
		}
		BlockEntityRenderState state = original.call(renderer);
		pooled.zakoopt$setState(state, renderer);
		return state;
	}
}
