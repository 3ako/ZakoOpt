package zako.opt.mixin.chunk;

import zako.opt.chunk.ParallelSkyLight;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSkyLightSources.class)
public class ChunkSkyLightSourcesMixin {
	@Inject(method = "fillFrom", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(ChunkAccess chunk, CallbackInfo ci) {
		if (ParallelSkyLight.fill(chunk, (ChunkSkyLightSources) (Object) this)) {
			ci.cancel();
		}
	}
}
