package zako.opt.block;

public interface CachedLight {
	int zakoopt$light();

	long zakoopt$lightTick();

	void zakoopt$setLight(int light, long tick);

	int zakoopt$pairLight();

	long zakoopt$pairLightTick();

	void zakoopt$setPairLight(int light, long tick);
}
