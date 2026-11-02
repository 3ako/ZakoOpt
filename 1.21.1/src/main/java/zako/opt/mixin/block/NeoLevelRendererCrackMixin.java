package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.block.CrackCache;

import java.util.SortedSet;

// NeoForge draws the crack through its ModelData overload of renderBreakingTexture
@Mixin(LevelRenderer.class)
public class NeoLevelRendererCrackMixin {
	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V"))
	private void zakoopt$cachedCrack(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack,
									 VertexConsumer consumer, ModelData data, Operation<Void> original) {
		if (!CrackCache.enabled()) {
			original.call(dispatcher, state, pos, level, poseStack, consumer, data);
			return;
		}
		long key = pos.asLong();
		if (CrackCache.stale(key, state, destructionProgress.get(key).last().getProgress())) {
			original.call(dispatcher, state, pos, level, CrackCache.origin(), CrackCache.recorder(), data);
			CrackCache.built(key);
		}
		CrackCache.queue(key, poseStack);
	}
}
