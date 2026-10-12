package zako.opt.feature.particles;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "particle_lod",
	group = FeatureGroup.PARTICLES,
	impact = Impact.MEDIUM,
	mixins = {"particle.QuadParticleGroupMixin", "particle.ParticleEngineLodMixin"}
)
public class ParticleLodFeature extends Feature {
}
