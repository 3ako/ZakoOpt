package zako.opt.particle;

import zako.opt.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class ParticleLod {
	// 1.21.4 only: the game time sampled once per ParticleEngine.render, which its light cache keys on. Later versions
	// read level.getGameTime() themselves; -1 means no frame has been sampled yet, and no cache key may match it
	public volatile long sampledTick = -1;

	public boolean keep(SingleQuadParticle particle, double x, double y, double z, Vec3 cam) {
		if (!Features.PARTICLE_LOD.on) {
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
