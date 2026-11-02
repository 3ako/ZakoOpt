package zako.opt.block;

import net.minecraft.world.level.block.state.BlockState;

public interface CachedValidity {
	BlockState zakoopt$validState();

	boolean zakoopt$valid();

	void zakoopt$setValid(BlockState state, boolean valid);

	long zakoopt$distanceTick();

	boolean zakoopt$inDistance();

	void zakoopt$setInDistance(boolean inDistance, long tick);
}
