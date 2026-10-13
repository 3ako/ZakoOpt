package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.gl.GpuTimers;

// 26.1.2's end of the vanilla profile returns it; the shared MinecraftTimerMixin skips the begin, so the end goes too
@Mixin(Minecraft.class)
public class MinecraftProfileEndTimerMixin {
	@WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;endProfile()Lcom/mojang/blaze3d/systems/TimerQuery$FrameProfile;"))
	private TimerQuery.FrameProfile zakoopt$ownTimersEnd(TimerQuery query, Operation<TimerQuery.FrameProfile> original) {
		return GpuTimers.supported() ? null : original.call(query);
	}
}
