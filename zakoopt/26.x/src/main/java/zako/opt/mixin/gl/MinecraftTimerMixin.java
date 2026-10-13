package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.GpuTimers;

// the frame's own elapsed-time query would overlap ours, and two of them cannot run at once; F3's GPU share comes from ours
@Mixin(Minecraft.class)
public class MinecraftTimerMixin {
	@Shadow
	private double gpuUtilization;

	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;beginProfile()V"))
	private void zakoopt$ownTimers(TimerQuery query, Operation<Void> original) {
		if (!GpuTimers.supported()) {
			original.call(query);
		}
	}

	// vanilla ends the profile it began under the same condition: with no begin, its end would throw
	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;endProfile()V"))
	private void zakoopt$ownTimersEnd(TimerQuery query, Operation<Void> original) {
		if (!GpuTimers.supported()) {
			original.call(query);
		}
	}

	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void zakoopt$utilization(CallbackInfo ci) {
		if (GpuTimers.supported()) {
			gpuUtilization = GpuTimers.utilization();
		}
	}
}
