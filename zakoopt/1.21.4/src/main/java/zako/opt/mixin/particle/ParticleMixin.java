package zako.opt.mixin.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.ZakoOptConfig;
import zako.opt.particle.ParticleLod;

@Mixin(Particle.class)
public class ParticleMixin {
	@Shadow
	@Final
	protected ClientLevel level;

	@Shadow
	protected boolean hasPhysics;
	@Shadow
	protected double x;
	@Shadow
	protected double y;
	@Shadow
	protected double z;

	@Unique
	private boolean zakoopt$physicsBefore;
	@Unique
	private long zakoopt$lightTick = Long.MIN_VALUE;
	@Unique
	private int zakoopt$light;

	// vanilla reads the non-interpolated x/y/z, which only change in tick(), so the value is constant within a tick
	@Inject(method = "getLightColor", at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		if (zakoopt$lightTick == ParticleLod.tick && ZakoOptConfig.particleLight()) {
			cir.setReturnValue(zakoopt$light);
		}
	}

	@Inject(method = "getLightColor", at = @At("RETURN"))
	private void zakoopt$storeLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
		zakoopt$light = cir.getReturnValueI();
		zakoopt$lightTick = ParticleLod.tick;
	}

	@Inject(method = "move", at = @At("HEAD"))
	private void zakoopt$cheapPhysics(double dx, double dy, double dz, CallbackInfo ci) {
		zakoopt$physicsBefore = hasPhysics;
		if (hasPhysics && ZakoOptConfig.particlePhysics() && zakoopt$skipCollision()) {
			hasPhysics = false;
		}
	}

	@Inject(method = "move", at = @At("RETURN"))
	private void zakoopt$restorePhysics(double dx, double dy, double dz, CallbackInfo ci) {
		hasPhysics = zakoopt$physicsBefore;
	}

	@Unique
	private boolean zakoopt$skipCollision() {
		Object self = this;
		if (self instanceof BaseAshSmokeParticle || self instanceof CampfireSmokeParticle || self instanceof BubbleColumnUpParticle) {
			return true;
		}
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		double ddx = x - cam.x, ddy = y - cam.y, ddz = z - cam.z;
		return ddx * ddx + ddy * ddy + ddz * ddz > 16 * 16;
	}
}
