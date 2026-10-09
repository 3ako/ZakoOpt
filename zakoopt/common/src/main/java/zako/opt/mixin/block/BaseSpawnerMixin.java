package zako.opt.mixin.block;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.TickCached;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin implements TickCached {
	@Unique
	private Object zakoopt$cached;
	@Unique
	private long zakoopt$cachedTick = Long.MIN_VALUE;

	@Override
	public void zakoopt$setCached(Object value, long tick) {
		zakoopt$cached = value;
		zakoopt$cachedTick = tick;
	}
}
