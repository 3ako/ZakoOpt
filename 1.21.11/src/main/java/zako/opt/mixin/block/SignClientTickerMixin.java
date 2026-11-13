package zako.opt.mixin.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.feature.Features;

// the sign ticker only drops a stale "player who may edit", which the client never has: on a client it was a tick per sign for nothing
@Mixin({SignBlock.class, CeilingHangingSignBlock.class, WallHangingSignBlock.class})
public class SignClientTickerMixin {
	@Inject(method = "getTicker", at = @At("HEAD"), cancellable = true)
	private void zakoopt$noClientTicker(Level level, BlockState state, BlockEntityType<?> type, CallbackInfoReturnable<BlockEntityTicker<?>> cir) {
		if (level.isClientSide() && Features.SIGN_CACHE.on) {
			cir.setReturnValue(null);
		}
	}
}
