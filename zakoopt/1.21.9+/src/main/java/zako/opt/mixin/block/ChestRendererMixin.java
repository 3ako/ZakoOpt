package zako.opt.mixin.block;

import zako.opt.feature.Features;

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
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import zako.opt.block.CachedLight;
import zako.opt.block.ChestFunctions;
import zako.opt.block.TickCached;

@Mixin(ChestRenderer.class)
public class ChestRendererMixin {
	@Unique
	private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V";

	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ChestBlock;combine(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/level/block/DoubleBlockCombiner$NeighborCombineResult;"))
	private DoubleBlockCombiner.NeighborCombineResult<?> zakoopt$cachedCombine(ChestBlock block, BlockState state, Level level, BlockPos pos, boolean ignoreBlocked,
																			Operation<DoubleBlockCombiner.NeighborCombineResult<?>> original,
																			BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
																			ModelFeatureRenderer.CrumblingOverlay crumbling) {
		if (!Features.BLOCK_ENTITY_CACHE.on) {
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
		if (!Features.MICRO_OPTS.stableResults || level == null) {
			return original.call(combiner, light);
		}
		CachedLight cache = (CachedLight) blockEntity;
		long tick = level.getGameTime();
		if (cache.zakoopt$pairLightTick() != tick) {
			cache.zakoopt$setPairLight(original.call(combiner, light), tick);
		}
		return cache.zakoopt$pairLight();
	}

	// render thread only; mulPose multiplies it into the pose at once and keeps nothing
	@Unique
	private static final Quaternionf ZAKOOPT$TURN = new Quaternionf();

	@WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;"))
	private Quaternionf zakoopt$turn(Axis axis, float degrees, Operation<Quaternionf> original) {
		return Features.FEWER_ALLOCATIONS.on && axis == Axis.YP ? ZAKOOPT$TURN.rotationY(degrees * Mth.DEG_TO_RAD) : original.call(axis, degrees);
	}

	// the lid and pair-light combiners and the functions they return are made anew per chest per frame, though they only tie the
	// two halves together: kept for the tick. A null combiner below means "this tick's function is kept, skip making one"
	@WrapOperation(method = EXTRACT, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/ChestBlock;opennessCombiner(Lnet/minecraft/world/level/block/entity/LidBlockEntity;)Lnet/minecraft/world/level/block/DoubleBlockCombiner$Combiner;"))
	private DoubleBlockCombiner.Combiner<?, ?> zakoopt$openCombiner(LidBlockEntity lid, Operation<DoubleBlockCombiner.Combiner<?, ?>> original,
																  BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
																  ModelFeatureRenderer.CrumblingOverlay crumbling) {
		Level level = blockEntity.getLevel();
		return Features.FEWER_ALLOCATIONS.on && level != null && ((ChestFunctions) blockEntity).zakoopt$openFn(level.getGameTime()) != null ? null : original.call(lid);
	}

	@WrapOperation(method = EXTRACT, at = @At(value = "NEW", target = "()Lnet/minecraft/client/renderer/blockentity/BrightnessCombiner;"))
	private BrightnessCombiner<?> zakoopt$lightCombiner(Operation<BrightnessCombiner<?>> original,
														BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
														ModelFeatureRenderer.CrumblingOverlay crumbling) {
		Level level = blockEntity.getLevel();
		return Features.FEWER_ALLOCATIONS.on && level != null && ((ChestFunctions) blockEntity).zakoopt$lightFn(level.getGameTime()) != null ? null : original.call();
	}

	@WrapOperation(method = EXTRACT, at = @At(value = "INVOKE", ordinal = 0,
			target = "Lnet/minecraft/world/level/block/DoubleBlockCombiner$NeighborCombineResult;apply(Lnet/minecraft/world/level/block/DoubleBlockCombiner$Combiner;)Ljava/lang/Object;"))
	private Object zakoopt$openFn(DoubleBlockCombiner.NeighborCombineResult<?> result, DoubleBlockCombiner.Combiner<?, ?> combiner, Operation<Object> original,
								  BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
								  ModelFeatureRenderer.CrumblingOverlay crumbling) {
		ChestFunctions cache = (ChestFunctions) blockEntity;
		Level level = blockEntity.getLevel();
		if (combiner == null) {
			return cache.zakoopt$openFn(level.getGameTime());
		}
		Object fn = original.call(result, combiner);
		if (Features.FEWER_ALLOCATIONS.on && level != null) {
			cache.zakoopt$setOpenFn(fn, level.getGameTime());
		}
		return fn;
	}

	@WrapOperation(method = EXTRACT, at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/world/level/block/DoubleBlockCombiner$NeighborCombineResult;apply(Lnet/minecraft/world/level/block/DoubleBlockCombiner$Combiner;)Ljava/lang/Object;"))
	private Object zakoopt$lightFn(DoubleBlockCombiner.NeighborCombineResult<?> result, DoubleBlockCombiner.Combiner<?, ?> combiner, Operation<Object> original,
								   BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
								   ModelFeatureRenderer.CrumblingOverlay crumbling) {
		ChestFunctions cache = (ChestFunctions) blockEntity;
		Level level = blockEntity.getLevel();
		if (combiner == null) {
			return cache.zakoopt$lightFn(level.getGameTime());
		}
		Object fn = original.call(result, combiner);
		if (Features.FEWER_ALLOCATIONS.on && level != null) {
			cache.zakoopt$setLightFn(fn, level.getGameTime());
		}
		return fn;
	}
}
