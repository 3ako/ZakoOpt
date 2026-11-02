package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import zako.opt.feature.Features;
import zako.opt.gl.OcclusionCull;

// arguments by type: Sodium's render takes different ones from version to version (fog in 0.7, a sampler in 0.8, 0.9.2 vs 0.9.3)
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public class SodiumOcclusionCullMixin {
	@Inject(require = 0, method = "render", at = @At("HEAD"))
	private void zakoopt$begin(CallbackInfo ci, @Local(argsOnly = true) TerrainRenderPass pass, @Local(argsOnly = true) CameraTransform camera,
			@Local(argsOnly = true) ChunkRenderMatrices matrices) {
		if (Features.OCCLUSION_CULL.on) {
			OcclusionCull.beginPass(pass, camera, matrices);
		}
	}

	@Inject(require = 0, method = "render", at = @At("RETURN"))
	private void zakoopt$test(CallbackInfo ci, @Local(argsOnly = true) ChunkRenderMatrices matrices, @Local(argsOnly = true) ChunkRenderListIterable lists,
			@Local(argsOnly = true) TerrainRenderPass pass, @Local(argsOnly = true) CameraTransform camera) {
		if (Features.OCCLUSION_CULL.on) {
			OcclusionCull.endPass(matrices, lists, pass, camera);
		}
	}

	@ModifyExpressionValue(require = 0, method = "fillCommandBuffer", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/ChunkRenderList;sectionsWithGeometryIterator(Z)Lnet/caffeinemc/mods/sodium/client/util/iterator/ByteIterator;"))
	private static ByteIterator zakoopt$skipHidden(ByteIterator sections, @Local(argsOnly = true) RenderRegion region) {
		return Features.OCCLUSION_CULL.on ? OcclusionCull.filter(sections, region) : sections;
	}
}
