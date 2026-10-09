package zako.opt.mixin.block;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.CachedLight;
import zako.opt.block.CachedValidity;
import zako.opt.block.TickCached;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(BlockEntity.class)
public class BlockEntityMixin implements TickCached, CachedLight, CachedValidity {
	@Unique
	private Object zakoopt$cached;
	@Unique
	private long zakoopt$cachedTick = Long.MIN_VALUE;
	@Unique
	private int zakoopt$light;
	@Unique
	private BlockState zakoopt$validState;
	@Unique
	private boolean zakoopt$valid;
	@Unique
	private long zakoopt$lightTick = Long.MIN_VALUE;
	@Unique
	private long zakoopt$distanceTick = Long.MIN_VALUE;
	@Unique
	private boolean zakoopt$inDistance;
	@Unique
	private int zakoopt$pairLight;
	@Unique
	private long zakoopt$pairLightTick = Long.MIN_VALUE;

	@Override
	public void zakoopt$setCached(Object value, long tick) {
		zakoopt$cached = value;
		zakoopt$cachedTick = tick;
	}

	@Override
	public void zakoopt$setLight(int light, long tick) {
		zakoopt$light = light;
		zakoopt$lightTick = tick;
	}

	@Override
	public void zakoopt$setPairLight(int light, long tick) {
		zakoopt$pairLight = light;
		zakoopt$pairLightTick = tick;
	}

	@Override
	public void zakoopt$setValid(BlockState state, boolean valid) {
		zakoopt$validState = state;
		zakoopt$valid = valid;
	}

	@Override
	public void zakoopt$setInDistance(boolean inDistance, long tick) {
		zakoopt$inDistance = inDistance;
		zakoopt$distanceTick = tick;
	}
}
