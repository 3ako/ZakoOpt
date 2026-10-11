package zako.opt.mixin.gl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.LunarMultiDraw;

// 26.x: Sodium draws terrain through the vanilla encoder, where Lunar keeps its flag
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class LunarMultiDrawMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void zakoopt$keepMultiDraw(CallbackInfo ci) {
		LunarMultiDraw.keep(this);
	}
}
