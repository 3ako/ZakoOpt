package zako.opt.mixin.particle;

import zako.opt.particle.GuardedRandom;
import zako.opt.particle.ILevelRandomGuard;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

// the level's shared random is read by several threads while the workers tick particles; Java 17+ forbids writing a
// final field from anywhere but <init>, so the field is de-finalized at apply time with @Mutable and wrapped in place
@Mixin(Level.class)
@Implements(@Interface(iface = ILevelRandomGuard.class, prefix = "zakoopt$"))
public abstract class LevelRandomMixin {
	@Shadow
	@Final
	@Mutable
	public RandomSource random;

	public void zakoopt$guardLevelRandom() {
		if (!(this.random instanceof GuardedRandom)) {
			this.random = new GuardedRandom(this.random);
		}
	}
}
