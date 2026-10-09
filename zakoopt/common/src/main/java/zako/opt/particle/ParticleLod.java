package zako.opt.particle;

import lombok.experimental.UtilityClass;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class ParticleLod {
	// game time sampled once per ParticleEngine.render; workers read it instead of chasing particle -> level per quad
	public long tick = -1;

	public boolean keep(SingleQuadParticle particle, double x, double y, double z, Vec3 cam) {
		if (!ZakoOptConfig.particleLod()) {
			return true;
		}
		double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
		double d2 = dx * dx + dy * dy + dz * dz;
		if (d2 < 16 * 16) {
			return true;
		}
		// identity hash keeps the choice stable per particle, so thinning doesn't flicker
		int h = System.identityHashCode(particle) >>> 4;
		return d2 < 32 * 32 ? (h & 1) == 0 : (h & 3) == 0;
	}
}
