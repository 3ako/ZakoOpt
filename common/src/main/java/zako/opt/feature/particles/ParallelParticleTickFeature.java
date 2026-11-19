package zako.opt.feature.particles;

import zako.opt.MixinPlugin;
import zako.opt.ZakoOptConfig;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

// the ticks themselves are thread-safe: every particle owns its random and reads the level. Spawns made by ticking
// particles wait in a thread-safe queue, and the engine's shared random is serialized while workers run
@FeatureData(
	key = "parallel_particle_tick",
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.ParticleGroupTickMixin", "particle.ParticleEngineSpawnMixin"}
)
public class ParallelParticleTickFeature extends Feature {
	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : MixinPlugin.ASYNC_PARTICLES ? "AsyncParticles" : null;
	}
}
