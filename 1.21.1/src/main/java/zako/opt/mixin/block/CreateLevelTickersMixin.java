package zako.opt.mixin.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.CreateFarTickers;

import java.util.List;

@Mixin(Level.class)
public class CreateLevelTickersMixin {
	@Shadow
	@Final
	protected List<TickingBlockEntity> blockEntityTickers;
	@Shadow
	@Final
	public boolean isClientSide;

	// before the loop walks the list: tickers moved out here are not walked at all
	@Inject(require = 0, method = "tickBlockEntities", at = @At("HEAD"))
	private void zakoopt$parkFar(CallbackInfo ci) {
		if (isClientSide) {
			CreateFarTickers.sweep((Level) (Object) this, blockEntityTickers);
		}
	}
}
