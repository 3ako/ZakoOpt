package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.block.MovingBlockCache;

@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererCacheMixin {
	@Shadow
	@Final
	private BlockRenderDispatcher blockRenderer;

	@Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cached(BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource buffers, Level level, boolean checkSides, int overlay,
								CallbackInfo ci) {
		if (ZakoOptConfig.movingBlockCache() && MovingBlockCache.render(blockRenderer, pos, state, poseStack, buffers, level, checkSides, overlay)) {
			ci.cancel();
		}
	}
}
