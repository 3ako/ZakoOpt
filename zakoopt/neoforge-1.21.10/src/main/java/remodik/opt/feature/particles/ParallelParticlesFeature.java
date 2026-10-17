package remodik.opt.feature.particles;

import remodik.opt.ZakoOptConfig;
import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;

// -Dzakoopt.parallelparticles=true forces it below 4 CPU threads, =auto follows the thread count like the switch does
@FeatureData(
	key = "parallel_particles",
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineParallelMixin"},
	bench = BenchView.HOLOGRAMS
)
public class ParallelParticlesFeature extends Feature {
	public boolean vertices;

	@Override
	public String blocked() {
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : null;
	}

	@Override
	public void refresh() {
		String p = System.getProperty(property());
		on = Features.active && (p == null ? value && blocked() == null : p.equals("true") || p.equals("auto") && blocked() == null);
		vertices = on && flag("parallelvertices", true);
	}
}
