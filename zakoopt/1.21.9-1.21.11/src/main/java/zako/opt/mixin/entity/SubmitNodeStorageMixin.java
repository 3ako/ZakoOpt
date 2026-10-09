package zako.opt.mixin.entity;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.ZakoOptConfig;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin {
	@Unique
	private int zakoopt$lastOrder;
	@Unique
	private SubmitNodeCollection zakoopt$last;

	@Inject(method = "order", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedOrder(int order, CallbackInfoReturnable<SubmitNodeCollection> cir) {
		if (zakoopt$last != null && zakoopt$lastOrder == order && ZakoOptConfig.microOpts2()) {
			cir.setReturnValue(zakoopt$last);
		}
	}

	@Inject(method = "order", at = @At("RETURN"))
	private void zakoopt$remember(int order, CallbackInfoReturnable<SubmitNodeCollection> cir) {
		zakoopt$lastOrder = order;
		zakoopt$last = cir.getReturnValue();
	}

	// endFrame drops unused collections from the map
	@Inject(method = "endFrame", at = @At("HEAD"))
	private void zakoopt$forget(CallbackInfo ci) {
		zakoopt$last = null;
	}
}
