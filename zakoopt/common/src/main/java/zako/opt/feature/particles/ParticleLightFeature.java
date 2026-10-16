package zako.opt.feature.particles;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_light",
	group = FeatureGroup.PARTICLES,
	mixins = "particle.ParticleMixin"
)
public class ParticleLightFeature extends Feature {
}
