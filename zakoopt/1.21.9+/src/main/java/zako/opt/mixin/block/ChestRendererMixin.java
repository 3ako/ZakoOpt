package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.CachedLight;
import zako.opt.block.TickCached;

@Mixin(ChestRenderer.class)
public class ChestRendererMixin {
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ChestBlock;combine(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/level/block/DoubleBlockCombiner$NeighborCombineResult;"))
	private DoubleBlockCombiner.NeighborCombineResult<?> zakoopt$cachedCombine(ChestBlock block, BlockState state, Level level, BlockPos pos, boolean ignoreBlocked,
																			Operation<DoubleBlockCombiner.NeighborCombineResult<?>> original,
																			BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
																			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		if (!ZakoOptConfig.blockEntityCache()) {
			return original.call(block, state, level, pos, ignoreBlocked);
		}
		// neighbour lookup for double chests; the result only changes when blocks change, so once per tick is enough
		TickCached cache = (TickCached) blockEntity;
		long tick = level.getGameTime();
		if (cache.zakoopt$cachedTick() != tick || cache.zakoopt$cached() == null) {
			cache.zakoopt$setCached(original.call(block, state, level, pos, ignoreBlocked), tick);
		}
		return (DoubleBlockCombiner.NeighborCombineResult<?>) cache.zakoopt$cached();
	}

	// double chest light: two light lookups per chest per frame, but light only changes on ticks
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2IntFunction;applyAsInt(I)I"))
	private int zakoopt$cachedPairLight(Int2IntFunction combiner, int light, Operation<Integer> original,
										BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
										ModelFeatureRenderer.CrumblingOverlay crumbling) {
		Level level = blockEntity.getLevel();
		if (!ZakoOptConfig.microOpts2() || level == null) {
			return original.call(combiner, light);
		}
		CachedLight cache = (CachedLight) blockEntity;
		long tick = level.getGameTime();
		if (cache.zakoopt$pairLightTick() != tick) {
			cache.zakoopt$setPairLight(original.call(combiner, light), tick);
		}
		return cache.zakoopt$pairLight();
	}
}
