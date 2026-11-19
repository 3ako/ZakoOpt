package zako.opt.mixin.gl;

import zako.opt.gl.GlCaches;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Sodium binds its own vertex arrays with raw GL calls behind vanilla's back: the shared cache must forget what it had
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice$ImmediateCommandList", remap = false)
public class SodiumDeviceCacheMixin {
	@Inject(method = {"bindVertexArray", "unbindVertexArray"}, at = @At("HEAD"))
	private void zakoopt$invalidateVao(CallbackInfo ci) {
		GlCaches.invalidateVao();
	}
}
