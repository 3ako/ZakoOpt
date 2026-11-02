package zako.opt.mixin.gl;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.GpuTimers;

@Mixin(LightTexture.class)
public class LightTextureTimerMixin {
	@Inject(method = "updateLightTexture", at = @At("HEAD"))
	private void zakoopt$markLightmap(CallbackInfo ci) {
		GpuTimers.mark("lightmap");
	}
}
