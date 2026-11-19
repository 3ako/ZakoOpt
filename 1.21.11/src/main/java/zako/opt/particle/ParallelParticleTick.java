package zako.opt.particle;

import lombok.experimental.UtilityClass;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.level.Level;
import zako.opt.feature.Features;
import zako.opt.mixin.particle.ParticleEngineLevelAccessor;
import zako.opt.mixin.particle.ParticleEngineUpdateCountInvoker;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@UtilityClass
public class ParallelParticleTick {
	private final int MIN_PARTICLES = 384;
	private final int BLOCK = 128;
	// every particle owns its random, so the ticks themselves never collide; only the engine's random, read while a
	// tick spawns new particles, is shared, and vanilla's threading detector would crash on two readers at once
	public final Object RANDOM_LOCK = new Object();
	// spawned particles wait here and are replayed on the render thread: the engine's staging queue is a plain ArrayDeque
	private final Queue<Particle> STAGED = new ConcurrentLinkedQueue<>();
	private Particle[] scratch = new Particle[0];
	private volatile boolean parallel;

	public boolean locking() {
		return Features.PARALLEL_PARTICLE_TICK.on;
	}

	// true while worker threads are ticking; ParticleEngine.add hands its callers over to the staging queue meanwhile
	public boolean stages(Particle particle) {
		if (!parallel) {
			return false;
		}
		STAGED.add(particle);
		return true;
	}

	// false = let the vanilla single-threaded loop run
	public boolean tick(Queue<? extends Particle> particles, ParticleEngine engine) {
		int n = particles.size();
		if (n < MIN_PARTICLES || !Features.PARALLEL_PARTICLE_TICK.on) {
			return false;
		}
		guardLevelRandom(engine);
		Particle[] arr = particles.toArray(scratch);
		scratch = arr;
		int blocks = (n + BLOCK - 1) / BLOCK;
		parallel = true;
		Throwable error;
		try {
			error = Workers.run(blocks, b -> tickRange(arr, b * BLOCK, Math.min(n, (b + 1) * BLOCK)));
		} finally {
			parallel = false;
		}
		// Workers.run only returns once every worker is done, so nothing joins the staging queue after this
		Particle staged;
		while ((staged = STAGED.poll()) != null) {
			engine.add(staged);
		}
		if (error != null) {
			throw new ReportedException(CrashReport.forThrowable(error, "Ticking Particle (zakoopt parallel)"));
		}
		return true;
	}

	private void tickRange(Particle[] arr, int from, int to) {
		for (int i = from; i < to; i++) {
			arr[i].tick();
		}
	}

	// a ticking particle can spawn another one, and the spawn's frequency gates read the level's shared random; with
	// two workers reading it vanilla's threading detector crashes the game (mclo.gs/A8umETo). The field is swapped for
	// a serialized wrapper once per level; every other reader keeps the same seed stream
	private void guardLevelRandom(ParticleEngine engine) {
		Level level = ((ParticleEngineLevelAccessor) engine).zakoopt$level();
		((ILevelRandomGuard) (Object) level).guardLevelRandom();
	}

	// the engine's counter is protected: the removal runs through an accessor mixin
	public void removeDead(Queue<? extends Particle> particles, ParticleEngine engine) {
		for (Iterator<? extends Particle> it = particles.iterator(); it.hasNext(); ) {
			Particle p = it.next();
			if (!p.isAlive()) {
				((ParticleEngineUpdateCountInvoker) engine).zakoopt$updateCount(p.getParticleLimit().orElse(null), -1);
				it.remove();
			}
		}
	}
}
