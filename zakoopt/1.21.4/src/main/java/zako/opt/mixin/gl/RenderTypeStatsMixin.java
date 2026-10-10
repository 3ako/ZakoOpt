package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.gl.DrawStats;

@Mixin(RenderType.class)
public class RenderTypeStatsMixin {
	@Inject(method = "draw", at = @At("HEAD"))
	private void zakoopt$countLayer(MeshData mesh, CallbackInfo ci) {
		DrawStats.layer(((RenderTypeAccessor) this).zakoopt$name());
	}
}
