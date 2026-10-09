package zako.opt.block;

public interface TickCached {
	Object zakoopt$cached();

	long zakoopt$cachedTick();

	void zakoopt$setCached(Object value, long tick);
}
