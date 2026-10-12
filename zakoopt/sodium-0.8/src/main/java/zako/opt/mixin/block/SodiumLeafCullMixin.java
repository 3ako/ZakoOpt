package zako.opt.mixin.block;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.block.LeafCulling;
import zako.opt.feature.Features;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class SodiumLeafCullMixin {
	@Shadow
	protected LevelSlice slice;
	@Shadow
	protected BlockState state;
	@Shadow
	protected BlockPos pos;

	@Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
	private void zakoopt$innerLeaves(Direction facing, CallbackInfoReturnable<Boolean> cir) {
		if (Features.LEAF_CULL.on && slice != null && LeafCulling.hidden(state, slice, pos, facing)) {
			cir.setReturnValue(false);
		}
	}
}
