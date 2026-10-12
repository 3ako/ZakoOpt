package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// a display without a culling size is never frustum culled, so every hologram is laid out and drawn each frame even behind
// the camera; it is culled by a box around it instead
@Mixin(Display.class)
public abstract class DisplayCullMixin {
	@Shadow
	private boolean noCulling;

	@Inject(method = "affectedByCulling", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cull(CallbackInfoReturnable<Boolean> cir) {
		if (noCulling && Features.DISPLAY_CULL.on) {
			cir.setReturnValue(true);
		}
	}

	// ponytail: a fixed 4-block box, text scaled past that can vanish at the screen edge; size it from the text if that shows up
	// 26.3 renamed the culling box getter, the other name simply does not match there
	@Inject(method = {"getBoundingBoxForCulling", "getInterpolatedBoundingBox"}, at = @At("HEAD"), cancellable = true)
	private void zakoopt$box(CallbackInfoReturnable<AABB> cir) {
		if (noCulling && Features.DISPLAY_CULL.on) {
			Display self = (Display) (Object) this;
			cir.setReturnValue(new AABB(self.getX() - 4, self.getY() - 4, self.getZ() - 4, self.getX() + 4, self.getY() + 4, self.getZ() + 4));
		}
	}
}
