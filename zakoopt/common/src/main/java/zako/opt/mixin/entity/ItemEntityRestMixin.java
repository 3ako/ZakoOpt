package zako.opt.mixin.entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

// an item lying still is ticked in full on the client every tick, though vanilla itself only moves it every 4th; the other 3
// keep just the age, which drives the bob and spin. A push arrives from the server as velocity and ends the rest
@Mixin(ItemEntity.class)
public abstract class ItemEntityRestMixin {
	@Shadow
	private int age;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void zakoopt$rest(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		// fire is drawn from a synced flag, no tick needed; reading that flag here cost more than the skip saved
		if ((self.tickCount + self.getId()) % 4 != 0 && ZakoOptConfig.restingItems() && self.onGround() && !self.isInWater()
				&& self.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-5F && self.level() instanceof ClientLevel) {
			if (age != -32768) {
				age++;
			}
			ci.cancel();
		}
	}
}
