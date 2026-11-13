package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.block.SignBoard;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public class SodiumSignMeshMixin {
	@WrapOperation(method = "execute", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;addBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Z)V"))
	private void zakoopt$bakeSign(BuiltSectionInfo.Builder data, BlockEntity blockEntity, boolean cull, Operation<Void> original,
								  @Local ChunkBuildBuffers buffers, @Local LevelSlice slice, @Local BlockEntityRenderer renderer) {
		if (!SignBoard.bake(blockEntity, renderer, buffers, slice)) {
			original.call(data, blockEntity, cull);
		}
	}
}
