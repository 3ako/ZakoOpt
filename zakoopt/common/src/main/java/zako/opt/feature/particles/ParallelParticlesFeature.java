package zako.opt.feature.particles;

import zako.opt.ZakoOptConfig;
import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Impact;

// -Dzakoopt.parallelparticles=true forces it below 4 CPU threads, =auto follows the thread count like the switch does
@FeatureData(
	key = "parallel_particles",
	group = FeatureGroup.PARTICLES,
	impact = Impact.HIGH,
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
