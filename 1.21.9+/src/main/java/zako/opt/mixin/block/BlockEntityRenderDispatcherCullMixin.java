package zako.opt.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.block.BlockEntityDistance;

// priority below the default 1000 so this HEAD hook runs before other mods' (Lunar's costs more than the test itself)
@Mixin(value = BlockEntityRenderDispatcher.class, priority = 500)
public class BlockEntityRenderDispatcherCullMixin {
	@Shadow
	private Vec3 cameraPos;

	// vanilla returns null for these further down: every check there only returns null, so checking distance first changes nothing
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void zakoopt$earlyCull(CallbackInfoReturnable<BlockEntityRenderState> cir, @Local(argsOnly = true) BlockEntity blockEntity) {
		if (BlockEntityDistance.culledEarly((BlockEntityRenderDispatcher) (Object) this, blockEntity, cameraPos)) {
			cir.setReturnValue(null);
		}
	}
}
