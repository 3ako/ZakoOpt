package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.FarLeaves;

// the render type of each quad is resolved in processQuad, from the model's own types; one renderer per builder thread
@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumFarLeavesMixin {
	@Unique
	private BlockState zakoopt$state;
	@Unique
	private BlockPos zakoopt$pos;

	@Inject(method = "renderModel", at = @At("HEAD"))
	private void zakoopt$block(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
		zakoopt$state = state;
		zakoopt$pos = pos;
	}

	@WrapOperation(method = "processQuad", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forRenderLayer(Lnet/minecraft/client/renderer/RenderType;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"))
	private Material zakoopt$farLeaves(RenderType type, Operation<Material> original) {
		return original.call(FarLeaves.type(zakoopt$state, zakoopt$pos, type));
	}
}
