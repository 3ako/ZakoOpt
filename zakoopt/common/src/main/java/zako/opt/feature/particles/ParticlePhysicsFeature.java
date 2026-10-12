package zako.opt.feature.particles;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "particle_physics",
	group = FeatureGroup.PARTICLES,
	impact = Impact.HIGH,
	mixins = "particle.ParticleMixin"
)
public class ParticlePhysicsFeature extends Feature {
}
