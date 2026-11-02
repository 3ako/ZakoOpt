package zako.opt.block;

// a moving piston's two moving-block states (the moved block and, for a retracting piston, its base), kept between frames
public interface MovingSlots {
	Object zakoopt$moving(int slot);

	void zakoopt$setMoving(int slot, Object state);
}
