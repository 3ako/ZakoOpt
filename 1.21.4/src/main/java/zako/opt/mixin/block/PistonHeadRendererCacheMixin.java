package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
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
import zako.opt.block.MovingBlockCache;

@Mixin(PistonHeadRenderer.class)
public class PistonHeadRendererCacheMixin {
	@Shadow
	@Final
	private BlockRenderDispatcher blockRenderer;

	// at the calls, not the head of renderBlock: a cancellable head made a CallbackInfo per moving block per frame
	@WrapWithCondition(method = "render(Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/PistonHeadRenderer;renderBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;ZI)V"))
	private boolean zakoopt$cached(PistonHeadRenderer self, BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource buffers, Level level,
								   boolean checkSides, int overlay) {
		return !Features.MOVING_BLOCK_CACHE.on || !MovingBlockCache.render(blockRenderer, pos, state, poseStack, buffers, level, checkSides, overlay);
	}
}
