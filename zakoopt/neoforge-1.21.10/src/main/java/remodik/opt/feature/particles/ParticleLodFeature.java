package remodik.opt.feature.particles;

import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_lod",
	visual = true,
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineLodMixin"}
)
public class ParticleLodFeature extends Feature {
}
