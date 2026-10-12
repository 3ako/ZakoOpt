package zako.opt.mixin.block;

import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.block.LeafCulling;
import zako.opt.feature.Features;

// Sodium up to 0.8.6 still culls faces here; 0.8.13 moved it into the render context, where SodiumLeafCullMixin hooks in
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class SodiumLeafCullOldMixin {
	@Inject(require = 0, method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
	private void zakoopt$innerLeaves(BlockState state, BlockGetter view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
		if (Features.LEAF_CULL.on && view instanceof LevelSlice slice && LeafCulling.hidden(state, slice, pos, facing)) {
			cir.setReturnValue(false);
		}
	}
}
