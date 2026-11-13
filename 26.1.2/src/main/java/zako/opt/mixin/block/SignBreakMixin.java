package zako.opt.mixin.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.SignBoard;

@Mixin(ClientLevel.class)
public class SignBreakMixin {
	@Inject(method = "destroyBlockProgress", at = @At("HEAD"))
	private void zakoopt$signBreaking(int id, BlockPos pos, int progress, CallbackInfo ci) {
		if (((ClientLevel) (Object) this).getBlockEntity(pos) instanceof SignBlockEntity) {
			boolean breaking = progress >= 0 && progress < 10;
			if (breaking ? SignBoard.BREAKING.add(pos.asLong()) : SignBoard.BREAKING.remove(pos.asLong())) {
				Minecraft.getInstance().levelRenderer.setSectionDirty(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()),
						SectionPos.blockToSectionCoord(pos.getZ()));
			}
		}
	}
}
