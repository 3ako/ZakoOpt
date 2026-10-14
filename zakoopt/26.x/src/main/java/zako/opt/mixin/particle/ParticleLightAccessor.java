package zako.opt.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// the light of a particle is read from the world, and a worker must not: zako.opt.particle.ParticleLight marks the pass
// where the value was already taken on the render thread
@Mixin(Particle.class)
public interface ParticleLightAccessor {
	// 26.x renamed the call
	@Invoker("getLightCoords")
	int zakoopt$light(float partialTick);
}
