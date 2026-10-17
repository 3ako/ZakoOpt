package remodik.opt.feature.particles;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_light",
	group = FeatureGroup.PARTICLES,
	mixins = "particle.ParticleMixin"
)
public class ParticleLightFeature extends Feature {
}
