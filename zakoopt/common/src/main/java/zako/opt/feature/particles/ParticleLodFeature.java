package zako.opt.feature.particles;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "particle_lod",
	visual = true,
	group = FeatureGroup.PARTICLES,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineLodMixin"}
)
public class ParticleLodFeature extends Feature {
}
