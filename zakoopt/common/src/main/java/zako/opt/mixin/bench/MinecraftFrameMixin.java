package zako.opt.mixin.bench;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.bench.Bench;

@Mixin(Minecraft.class)
public class MinecraftFrameMixin {
	@Inject(method = "runTick", at = @At("HEAD"))
	private void zakoopt$frame(boolean advanceGameTime, CallbackInfo ci) {
		Bench.frame(System.nanoTime());
	}
}
