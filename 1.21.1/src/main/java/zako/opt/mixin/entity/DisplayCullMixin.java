package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1.21.1: displays skip the frustum test through Entity.noCulling, which DisplayCullRendererMixin takes back; the box has to
// come from the display itself, since Sodium's own visibility check asks it directly
@Mixin(Display.class)
public abstract class DisplayCullMixin {
	// ponytail: a fixed 4-block box, text scaled past that can vanish at the screen edge; size it from the text if that shows up
	@Inject(method = "getBoundingBoxForCulling", at = @At("HEAD"), cancellable = true)
	private void zakoopt$box(CallbackInfoReturnable<AABB> cir) {
		Display self = (Display) (Object) this;
		if (self.noCulling && Features.DISPLAY_CULL.on) {
			cir.setReturnValue(new AABB(self.getX() - 4, self.getY() - 4, self.getZ() - 4, self.getX() + 4, self.getY() + 4, self.getZ() + 4));
		}
	}
}
