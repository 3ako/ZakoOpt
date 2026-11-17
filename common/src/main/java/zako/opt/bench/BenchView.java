package zako.opt.bench;

// the view of the test scene a feature is measured on, in the order the camera visits them
public enum BenchView {
	NONE,
	TERRAIN,
	OVERVIEW,
	CROWD,
	ITEMS,
	BLOCKS,
	PISTONS,
	HOLOGRAMS;

	public String key() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
