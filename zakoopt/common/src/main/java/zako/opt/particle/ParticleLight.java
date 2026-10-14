package zako.opt.particle;

import lombok.experimental.UtilityClass;

// While the parallel pass runs, a particle's light is not read from the world: the light engine and the chunk cache are
// written by other threads while the game renders, so the value is taken on the render thread instead and the workers get
// it back from the particle's own cache. One flag per thread, since a block of particles may run on the render thread and
// the next one on a helper
@UtilityClass
public class ParticleLight {
	private final ThreadLocal<Boolean> PARALLEL = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public boolean parallel() {
		return PARALLEL.get();
	}

	public void parallel(boolean on) {
		PARALLEL.set(on);
	}
}
