package zako.opt.particle;

// a plain interface merged into Level by LevelRandomMixin; the cast happens at the swap call site
public interface ILevelRandomGuard {
	void guardLevelRandom();
}
