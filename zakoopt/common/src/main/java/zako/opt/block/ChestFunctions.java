package zako.opt.block;

// a chest's lid and pair-light functions for the current tick; they only tie the two halves together, the partial tick is
// applied to them every frame
public interface ChestFunctions {
	Object zakoopt$openFn(long tick);

	Object zakoopt$lightFn(long tick);

	void zakoopt$setOpenFn(Object fn, long tick);

	void zakoopt$setLightFn(Object fn, long tick);
}
