package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.state.PistonHeadRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.block.MovingSlots;

// a moving piston makes one or two moving-block states per frame, each with every field set: kept on the block entity. The
// state's block is cleared first and the base is only made after the block, so a set block means this call makes the base
@Mixin(PistonHeadRenderer.class)
public class PistonMovingStateMixin {
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/PistonHeadRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;createMovingBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/Level;)Lnet/minecraft/client/renderer/block/MovingBlockRenderState;"))
	private MovingBlockRenderState zakoopt$keptState(BlockPos pos, BlockState blockState, Holder<Biome> biome, Level level, Operation<MovingBlockRenderState> original,
													 PistonMovingBlockEntity piston, PistonHeadRenderState state, float partialTick, Vec3 camera,
													 ModelFeatureRenderer.CrumblingOverlay crumbling) {
		if (!Features.FEWER_ALLOCATIONS.on) {
			return original.call(pos, blockState, biome, level);
		}
		int slot = state.block == null ? 0 : 1;
		MovingSlots slots = (MovingSlots) piston;
		if (!(slots.zakoopt$moving(slot) instanceof MovingBlockRenderState kept)) {
			MovingBlockRenderState made = original.call(pos, blockState, biome, level);
			slots.zakoopt$setMoving(slot, made);
			return made;
		}
		kept.randomSeedPos = pos;
		kept.blockPos = pos;
		kept.blockState = blockState;
		kept.biome = biome;
		kept.level = level;
		return kept;
	}
}
