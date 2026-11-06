package zako.opt.mixin.block;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.ChestBake;

// progress 0..9 is a crack being drawn, anything else clears it; handled here, as Lithium stops the tickers of shut chests
@Mixin(ClientLevel.class)
public class ClientLevelChestBreakMixin {
	@Inject(method = "destroyBlockProgress", at = @At("HEAD"))
	private void zakoopt$breaking(int id, BlockPos pos, int progress, CallbackInfo ci) {
		if (((ClientLevel) (Object) this).getBlockEntity(pos) instanceof ChestBake chest) {
			ChestBake.breaking(pos, chest, progress >= 0 && progress < 10);
		}
	}
}
