package remodik.opt.mixin.gl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import remodik.opt.gl.LunarMultiDraw;

// Sodium up to 0.8 draws terrain through its own command list, made anew for each draw pass; Lunar keeps its flag on each
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice$ImmediateDrawCommandList")
public class LunarSodiumMultiDrawMixin {
	@Inject(method = "<init>", at = @At("RETURN"), require = 0)
	private void zakoopt$keepMultiDraw(CallbackInfo ci) {
		LunarMultiDraw.keep(this);
	}
}
