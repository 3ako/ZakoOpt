package zako.opt.mixin.block;

import net.minecraft.world.level.block.entity.BlockEntityTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public interface CreateBoundTickerAccessor {
	@Accessor("ticker")
	BlockEntityTicker<?> zakoopt$ticker();
}
