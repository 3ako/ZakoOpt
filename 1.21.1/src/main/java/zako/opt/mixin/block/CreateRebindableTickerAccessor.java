package zako.opt.mixin.block;

import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$RebindableTickingBlockEntityWrapper")
public interface CreateRebindableTickerAccessor {
	@Accessor("ticker")
	TickingBlockEntity zakoopt$ticker();
}
