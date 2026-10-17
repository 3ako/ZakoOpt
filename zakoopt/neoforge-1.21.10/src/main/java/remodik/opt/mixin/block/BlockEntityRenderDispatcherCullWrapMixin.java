package remodik.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import remodik.opt.block.BlockEntityDistance;

// the early cull without Lunar: the renderer lookup is the method's first step and a null renderer returns null at once, so a
// renderer out of distance comes back as null; unlike the HEAD hook it makes no CallbackInfoReturnable per block entity
// per frame. With Lunar the HEAD hook stays, so the test runs before Lunar's own costly hook
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherCullWrapMixin {
	@Shadow
	private Vec3 cameraPos;

	@WrapOperation(method = "tryExtractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;FLnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
	private BlockEntityRenderer<?, ?> zakoopt$earlyCullRenderer(BlockEntityRenderDispatcher self, BlockEntity blockEntity, Operation<BlockEntityRenderer<?, ?>> original) {
		BlockEntityRenderer<?, ?> renderer = original.call(self, blockEntity);
		return BlockEntityDistance.culled(renderer, blockEntity, cameraPos) ? null : renderer;
	}
}
