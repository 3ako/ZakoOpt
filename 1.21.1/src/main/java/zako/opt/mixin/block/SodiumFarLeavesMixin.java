package zako.opt.mixin.block;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.FarLeaves;

// the render type of each quad is resolved in processQuad, from the model's own types; one renderer per builder thread
// priority above the default: BetterBlockEntities overwrites processQuad at 1000, and Mixin lets only a later mixin
// inject into a method another one merged
@Mixin(value = BlockRenderer.class, remap = false, priority = 1100)
public class SodiumFarLeavesMixin {
	@Unique
	private BlockState zakoopt$state;
	@Unique
	private BlockPos zakoopt$pos;
	@Unique
	private final FarLeaves.Section zakoopt$section = new FarLeaves.Section();

	@Inject(require = 0, method = "renderModel", at = @At("HEAD"))
	private void zakoopt$block(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
		zakoopt$state = state;
		zakoopt$pos = pos;
	}

	// on every quad of every block: only the layer argument is swapped, no argument array or wrapper object
	@ModifyArg(require = 0, method = "processQuad", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forRenderLayer(Lnet/minecraft/client/renderer/RenderType;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"))
	private RenderType zakoopt$farLeaves(RenderType type) {
		return FarLeaves.type(zakoopt$section, zakoopt$state, zakoopt$pos, type);
	}
}
