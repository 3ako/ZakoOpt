package zako.opt.mixin.block;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.CachedLight;
import zako.opt.block.CachedValidity;
import zako.opt.block.ChestFunctions;
import zako.opt.block.MovingSlots;
import zako.opt.block.TickCached;
import zako.opt.entity.PooledState;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(BlockEntity.class)
public class BlockEntityMixin implements TickCached, CachedLight, CachedValidity, PooledState, ChestFunctions, MovingSlots {
	@Unique
	private Object zakoopt$moving0;
	@Unique
	private Object zakoopt$moving1;
	@Unique
	private Object zakoopt$chestOpen;
	@Unique
	private Object zakoopt$chestLight;
	@Unique
	private long zakoopt$chestTick = Long.MIN_VALUE;
	@Unique
	private Object zakoopt$state;
	@Unique
	private Object zakoopt$stateOwner;
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
	public Object zakoopt$moving(int slot) {
		return slot == 0 ? zakoopt$moving0 : zakoopt$moving1;
	}

	@Override
	public void zakoopt$setMoving(int slot, Object state) {
		if (slot == 0) {
			zakoopt$moving0 = state;
		} else {
			zakoopt$moving1 = state;
		}
	}

	@Override
	public Object zakoopt$openFn(long tick) {
		return zakoopt$chestTick == tick ? zakoopt$chestOpen : null;
	}

	@Override
	public Object zakoopt$lightFn(long tick) {
		return zakoopt$chestTick == tick ? zakoopt$chestLight : null;
	}

	@Override
	public void zakoopt$setOpenFn(Object fn, long tick) {
		if (zakoopt$chestTick != tick) {
			zakoopt$chestTick = tick;
			zakoopt$chestLight = null;
		}
		zakoopt$chestOpen = fn;
	}

	@Override
	public void zakoopt$setLightFn(Object fn, long tick) {
		if (zakoopt$chestTick != tick) {
			zakoopt$chestTick = tick;
			zakoopt$chestOpen = null;
		}
		zakoopt$chestLight = fn;
	}

	@Override
	public void zakoopt$setState(Object state, Object owner) {
		zakoopt$state = state;
		zakoopt$stateOwner = owner;
	}

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
