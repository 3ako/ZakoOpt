package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.util.color.FastCubicSampler;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.gl.CubicColors;

import java.util.function.Function;

@Mixin(value = FastCubicSampler.class, remap = false)
public class SodiumCubicSamplerMixin {
	@Inject(method = "sampleColor", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$fewerColors(Vec3 pos, FastCubicSampler.ColorFetcher fetcher, Function<Vec3, Vec3> transformer, CallbackInfoReturnable<Vec3> cir) {
		if (Features.FEWER_ALLOCATIONS.on && RenderSystem.isOnRenderThread()) {
			cir.setReturnValue(CubicColors.sample(pos, fetcher, transformer));
		}
	}
}
