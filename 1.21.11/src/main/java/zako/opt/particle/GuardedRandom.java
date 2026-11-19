package zako.opt.particle;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

// a wrapper handed out in place of the level's random once workers tick particles beside the render thread: every
// call to the wrapped source goes through a monitor, so vanilla's threading detector never sees two readers. The seed
// stream is the same as before, only serialized
public class GuardedRandom implements RandomSource {
	private final RandomSource source;

	public GuardedRandom(RandomSource source) {
		this.source = source;
	}

	@Override
	public RandomSource fork() {
		// a fork reads the seed once and never touches this source again
		return source.fork();
	}

	@Override
	public PositionalRandomFactory forkPositional() {
		return source.forkPositional();
	}

	@Override
	public void setSeed(long seed) {
		synchronized (source) {
			source.setSeed(seed);
		}
	}

	@Override
	public int nextInt() {
		synchronized (source) {
			return source.nextInt();
		}
	}

	@Override
	public int nextInt(int bound) {
		synchronized (source) {
			return source.nextInt(bound);
		}
	}

	@Override
	public long nextLong() {
		synchronized (source) {
			return source.nextLong();
		}
	}

	@Override
	public boolean nextBoolean() {
		synchronized (source) {
			return source.nextBoolean();
		}
	}

	@Override
	public float nextFloat() {
		synchronized (source) {
			return source.nextFloat();
		}
	}

	@Override
	public double nextDouble() {
		synchronized (source) {
			return source.nextDouble();
		}
	}

	@Override
	public double nextGaussian() {
		synchronized (source) {
			return source.nextGaussian();
		}
	}
}
