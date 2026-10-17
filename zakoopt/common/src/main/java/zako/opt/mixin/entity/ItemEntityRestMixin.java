package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// an item lying still is ticked in full on the client every tick, though vanilla itself only moves it every 4th; the rest
// keep just the age, which drives the bob and spin. A push arrives from the server as velocity, a fall as onGround in its move
// packet, and either ends the rest. Even vanilla's every-4th move was most of a pile's cost: its collision check scans every
// entity around (60% of an item's tick in a 1200-item stash, profiled), so a full tick is left only every 20th, a multiple of
// vanilla's 4 so it still moves there, to catch what the server does not send
@Mixin(ItemEntity.class)
public abstract class ItemEntityRestMixin {
	@Unique
	private static final int ZAKOOPT$FULL_TICK = 20;
	@Shadow
	private int age;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void zakoopt$rest(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		// fire is drawn from a synced flag, no tick needed; reading that flag here cost more than the skip saved
		if ((self.tickCount + self.getId()) % ZAKOOPT$FULL_TICK != 0 && Features.RESTING_ITEMS.on && self.onGround() && !self.isInWater()
				&& self.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-5F && self.level() instanceof ClientLevel) {
			if (age != -32768) {
				age++;
			}
			ci.cancel();
		}
	}
}
