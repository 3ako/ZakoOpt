package zako.opt.mixin.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParticleEngine.class)
public interface ParticleEngineUpdateCountInvoker {
	@Invoker("updateCount")
	void zakoopt$updateCount(ParticleLimit limit, int count);
}
