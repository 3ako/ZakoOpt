package zako.opt.mixin.block;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.ChestBake;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityBakeMixin implements ChestBake {
	@Unique
	private volatile byte zakoopt$mesh;
	@Unique
	private volatile boolean zakoopt$breaking;

	@Inject(method = "lidAnimateTick", at = @At("TAIL"))
	private static void zakoopt$bake(Level level, BlockPos pos, BlockState state, ChestBlockEntity blockEntity, CallbackInfo ci) {
		ChestBake.tick(pos, blockEntity);
	}
}
