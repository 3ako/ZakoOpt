package zako.opt.text;

// a cache that mostly misses (more lines around than it holds, or text rebuilt every frame) inserts and evicts on every
// lookup and costs more than it saves: it rests a while, then tries again
public class HitRate {
	private static final int WINDOW = 4096;
	private static final long REST_NS = 2_000_000_000L;
	private int lookups, hits;
	private long restUntil;

	public boolean resting() {
		return System.nanoTime() < restUntil;
	}

	public void count(boolean hit) {
		if (hit) {
			hits++;
		}
		if (++lookups >= WINDOW) {
			if (hits * 2 < lookups) {
				restUntil = System.nanoTime() + REST_NS;
			}
			lookups = hits = 0;
		}
	}
}
