package zako.opt.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.SignBoard;
import zako.opt.text.SignTextMesh;

@Mixin(BlockEntity.class)
public class SignTextRemoveMixin {
	@Shadow
	@Final
	protected BlockPos worldPosition;
	@Shadow
	@Nullable
	protected Level level;

	@Inject(method = "setRemoved", at = @At("TAIL"))
	private void zakoopt$forgetSignText(CallbackInfo ci) {
		if ((Object) this instanceof SignBlockEntity && level != null && level.isClientSide()) {
			SignTextMesh.remove(worldPosition);
			SignBoard.BREAKING.remove(worldPosition.asLong());
		}
	}
}
