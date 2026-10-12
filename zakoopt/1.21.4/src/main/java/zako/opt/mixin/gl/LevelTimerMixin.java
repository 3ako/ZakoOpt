package zako.opt.mixin.gl;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.GpuTimers;

@Mixin(LevelRenderer.class)
public class LevelTimerMixin {
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void zakoopt$markSetup(CallbackInfo ci) {
		GpuTimers.mark("terrain setup, chunk uploads");
	}
}
