package zako.opt.mixin.block;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.PistonHeadRenderer;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.SpawnerRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.PooledState;

// a block entity's render state is made anew every frame and dropped with the frame; one per block entity is enough. Only
// renderers whose extraction writes every field: a chest and a moving piston always, a spawner except its mob, which is cleared first.
// Exact classes, since a mod's subclass may keep fields of its own that its extraction sets only sometimes
@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderStatePoolMixin {
	@WrapOperation(method = "tryExtractRenderState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"))
	private BlockEntityRenderState zakoopt$pooled(BlockEntityRenderer<?, ?> renderer, Operation<BlockEntityRenderState> original,
												  @Local(argsOnly = true) BlockEntity blockEntity) {
		Class<?> type = renderer.getClass();
		if (!Features.FEWER_ALLOCATIONS.on || type != ChestRenderer.class && type != SpawnerRenderer.class && type != PistonHeadRenderer.class) {
			return original.call(renderer);
		}
		PooledState pooled = (PooledState) blockEntity;
		if (pooled.zakoopt$stateOwner() == renderer) {
			BlockEntityRenderState state = (BlockEntityRenderState) pooled.zakoopt$state();
			if (state instanceof SpawnerRenderState spawner) {
				spawner.displayEntity = null;
			}
			return state;
		}
		BlockEntityRenderState state = original.call(renderer);
		pooled.zakoopt$setState(state, renderer);
		return state;
	}
}
