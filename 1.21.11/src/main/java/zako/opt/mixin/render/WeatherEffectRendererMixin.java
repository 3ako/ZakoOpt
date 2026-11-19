package zako.opt.mixin.render;

import zako.opt.render.ParallelWeather;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(Level level, int ticks, float partialTick, Vec3 camera, WeatherRenderState state, CallbackInfo ci) {
		if (ParallelWeather.extract((WeatherEffectRenderer) (Object) this, level, ticks, partialTick, camera, state)) {
			ci.cancel();
		}
	}
}
