package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.FarLeaves;

// the layer of each quad is resolved in processQuad; one renderer per builder thread
@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumFarLeavesMixin {
	@Unique
	private BlockState zakoopt$state;
	@Unique
	private BlockPos zakoopt$pos;

	@Inject(require = 0, method = "renderModel", at = @At("HEAD"))
	private void zakoopt$block(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
		zakoopt$state = state;
		zakoopt$pos = pos;
	}

	@WrapOperation(require = 0, method = "processQuad", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forChunkLayer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"))
	private Material zakoopt$farLeaves(ChunkSectionLayer layer, Operation<Material> original) {
		return original.call(FarLeaves.type(zakoopt$state, zakoopt$pos, layer));
	}
}
