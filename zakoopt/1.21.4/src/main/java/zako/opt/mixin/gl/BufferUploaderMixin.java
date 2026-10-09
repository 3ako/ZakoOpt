package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.ImmediateRing;

@Mixin(BufferUploader.class)
public class BufferUploaderMixin {
	@Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$ringWithShader(MeshData mesh, CallbackInfo ci) {
		if (ImmediateRing.draw(mesh, true)) {
			ci.cancel();
		}
	}

	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$ring(MeshData mesh, CallbackInfo ci) {
		if (ImmediateRing.draw(mesh, false)) {
			ci.cancel();
		}
	}
}
