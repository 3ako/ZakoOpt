package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// the crosshair's block and entity search repeats every frame even for a still camera; while the eye and the view stand
// still the last result is just kept, and any movement runs the search again
@Mixin(GameRenderer.class)
public class GameRendererPickMixin {
	@Shadow
	@Final
	public Minecraft minecraft;

	@Unique
	private int zakoopt$frames;
	@Unique
	private double zakoopt$lastX = Double.NaN;
	@Unique
	private double zakoopt$lastY;
	@Unique
	private double zakoopt$lastZ;
	@Unique
	private float zakoopt$lastYaw = Float.NaN;
	@Unique
	private float zakoopt$lastPitch;

	@Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true)
	private void zakoopt$skipWhileStill(float f, CallbackInfo ci) {
		if (!Features.PICK_THROTTLE.on) {
			return;
		}
		Entity entity = this.minecraft.getCameraEntity();
		if (entity == null || this.minecraft.level == null) {
			return;
		}

		double x = entity.getEyePosition(f).x();
		double y = entity.getEyePosition(f).y();
		double z = entity.getEyePosition(f).z();
		float yaw = entity.getViewYRot(f);
		float pitch = entity.getViewXRot(f);
		boolean moved = x != this.zakoopt$lastX || y != this.zakoopt$lastY || z != this.zakoopt$lastZ
			|| yaw != this.zakoopt$lastYaw || pitch != this.zakoopt$lastPitch;
		if (moved) {
			this.zakoopt$lastX = x;
			this.zakoopt$lastY = y;
			this.zakoopt$lastZ = z;
			this.zakoopt$lastYaw = yaw;
			this.zakoopt$lastPitch = pitch;
			return;
		}

		if ((this.zakoopt$frames++ & 1) != 0) {
			ci.cancel();
		}
	}
}
