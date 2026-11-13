package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.text.SignTextMesh;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public class SodiumSignMeshMixin {
	@WrapOperation(method = "execute", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/BuiltSectionInfo$Builder;addBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Z)V"))
	private void zakoopt$bakeSign(BuiltSectionInfo.Builder data, BlockEntity blockEntity, boolean cull, Operation<Void> original, @Local BlockEntityRenderer renderer) {
		if (!SignTextMesh.bake(blockEntity, renderer)) {
			original.call(data, blockEntity, cull);
		}
	}
}
