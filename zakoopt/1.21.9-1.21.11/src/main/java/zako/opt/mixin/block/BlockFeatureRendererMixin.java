package zako.opt.mixin.block;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.MovingBlockCache;

@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {
	// must run before Fabric's moving-block loop takes the iterator
	@Inject(method = "render", at = @At("HEAD"))
	private void zakoopt$cachedMovingBlocks(SubmitNodeCollection collection, MultiBufferSource.BufferSource buffers, BlockRenderDispatcher dispatcher,
											OutlineBufferSource outlines, CallbackInfo ci) {
		MovingBlockCache.render(collection.getMovingBlockSubmits(), buffers, dispatcher);
	}
}
