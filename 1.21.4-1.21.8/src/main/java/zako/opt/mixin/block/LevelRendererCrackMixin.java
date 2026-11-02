package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.CrackCache;

import java.util.SortedSet;

// 1.21.4 to 1.21.8 draw the cracks in renderBlockDestroyAnimation, handed the crumbling buffer source
@Mixin(LevelRenderer.class)
public class LevelRendererCrackMixin {
	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	// a crack's getBuffer switched the immediate buffer to its stage, an upload and a draw per block; queued cracks draw nothing here
	@WrapOperation(method = "renderBlockDestroyAnimation", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer zakoopt$noSwitch(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original) {
		return CrackCache.enabled() && CrackCache.hooked ? CrackCache.IDLE : original.call(source, type);
	}

	@WrapOperation(method = "renderBlockDestroyAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
	private void zakoopt$cachedCrack(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack,
									 VertexConsumer consumer, Operation<Void> original) {
		if (!CrackCache.enabled()) {
			original.call(dispatcher, state, pos, level, poseStack, consumer);
			return;
		}
		long key = pos.asLong();
		if (CrackCache.stale(key, state, destructionProgress.get(key).last().getProgress())) {
			original.call(dispatcher, state, pos, level, CrackCache.origin(), CrackCache.recorder());
			CrackCache.built(key);
		}
		CrackCache.queue(key, poseStack);
	}

	@Inject(method = "renderBlockDestroyAnimation", at = @At("RETURN"))
	private void zakoopt$flushCracks(PoseStack poseStack, Camera camera, MultiBufferSource.BufferSource crumbling, CallbackInfo ci) {
		CrackCache.flush(crumbling);
	}

	// resource reloads rebuild the block models
	@Inject(method = "allChanged", at = @At("HEAD"))
	private void zakoopt$clearCracks(CallbackInfo ci) {
		CrackCache.clear();
	}
}
