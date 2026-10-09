package zako.opt.mixin.gl;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.AnimFreeze;
import zako.opt.gl.FrameFence;
import zako.opt.entity.SkinAtlas;

@Mixin(Minecraft.class)
public class MinecraftFrameMixin {
	@Inject(method = "renderFrame", at = @At("HEAD"))
	private void zakoopt$frame(boolean advanceGameTime, CallbackInfo ci) {
		FrameFence.endFrame();
		SkinAtlas.endFrame();
		AnimFreeze.frame++;
		ZakoOptConfig.refresh();
	}
}
