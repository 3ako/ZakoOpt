package zako.opt.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
	@Accessor("x")
	double zakoopt$x();

	@Accessor("y")
	double zakoopt$y();

	@Accessor("z")
	double zakoopt$z();
}
