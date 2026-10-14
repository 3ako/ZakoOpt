package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import zako.opt.gl.GpuTimers;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public class SodiumTerrainTimerMixin {
	@Inject(require = 0, method = "render", at = @At("HEAD"))
	private void zakoopt$markTerrain(CallbackInfo ci, @Local(argsOnly = true) TerrainRenderPass pass) {
		GpuTimers.mark(pass == DefaultTerrainRenderPasses.SOLID ? "terrain solid" : pass == DefaultTerrainRenderPasses.CUTOUT ? "terrain cutout" : "terrain translucent");
	}

	@Inject(require = 0, method = "render", at = @At("RETURN"))
	private void zakoopt$markAfterTerrain(CallbackInfo ci, @Local(argsOnly = true) TerrainRenderPass pass) {
		GpuTimers.mark(pass == DefaultTerrainRenderPasses.SOLID ? "after solid" : pass == DefaultTerrainRenderPasses.CUTOUT ? "entities, block entities" : "after translucent");
	}
}
