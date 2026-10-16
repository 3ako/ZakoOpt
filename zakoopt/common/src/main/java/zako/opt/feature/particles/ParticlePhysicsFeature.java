package zako.opt.feature.particles;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_physics",
	visual = true,
	group = FeatureGroup.PARTICLES,
	mixins = "particle.ParticleMixin"
)
public class ParticlePhysicsFeature extends Feature {
}
