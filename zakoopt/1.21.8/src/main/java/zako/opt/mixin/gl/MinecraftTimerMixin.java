package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.gl.GpuTimers;

import java.util.Optional;

// the frame's own elapsed-time query would overlap ours, and two of them cannot run at once; F3's GPU share comes from ours
@Mixin(Minecraft.class)
public class MinecraftTimerMixin {
	@Shadow
	private double gpuUtilization;

	@WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;getInstance()Ljava/util/Optional;"))
	private Optional<TimerQuery> zakoopt$ownTimers(Operation<Optional<TimerQuery>> original) {
		if (GpuTimers.supported()) {
			gpuUtilization = GpuTimers.utilization();
			return Optional.empty();
		}
		return original.call();
	}
}
