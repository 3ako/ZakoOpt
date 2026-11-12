package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// the scale is an attribute lookup per entity per frame; on the client it is kept for a game tick
@Mixin(LivingEntity.class)
public class LivingEntityScaleMixin {
	@Unique
	private long zakoopt$scaleTime = Long.MIN_VALUE;
	@Unique
	private float zakoopt$scale;

	@Inject(method = "getScale", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedScale(CallbackInfoReturnable<Float> cir) {
		if (Features.LOOKUP_CACHES.on && zakoopt$scaleTime == ((LivingEntity) (Object) this).level().getGameTime()
				&& ((LivingEntity) (Object) this).level().isClientSide()) {
			cir.setReturnValue(zakoopt$scale);
		}
	}

	@Inject(method = "getScale", at = @At("RETURN"))
	private void zakoopt$keepScale(CallbackInfoReturnable<Float> cir) {
		zakoopt$scale = cir.getReturnValueF();
		zakoopt$scaleTime = ((LivingEntity) (Object) this).level().getGameTime();
	}
}
