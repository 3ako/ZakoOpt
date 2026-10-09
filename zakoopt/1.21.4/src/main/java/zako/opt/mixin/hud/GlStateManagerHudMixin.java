package zako.opt.mixin.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.hud.HudCache;

@Mixin(GlStateManager.class)
public class GlStateManagerHudMixin {
	// vanilla GUI blending writes alpha with (ONE, ZERO): a translucent quad over an opaque one would punch a hole in the HUD texture
	@ModifyVariable(method = "_blendFuncSeparate", at = @At("HEAD"), ordinal = 2, argsOnly = true)
	private static int zakoopt$srcAlpha(int value) {
		return HudCache.drawing ? GL11.GL_ONE : value;
	}

	@ModifyVariable(method = "_blendFuncSeparate", at = @At("HEAD"), ordinal = 3, argsOnly = true)
	private static int zakoopt$dstAlpha(int value) {
		return HudCache.drawing ? GL11.GL_ONE_MINUS_SRC_ALPHA : value;
	}

	@Inject(method = "_blendFunc", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$separate(int src, int dst, CallbackInfo ci) {
		if (HudCache.drawing) {
			GlStateManager._blendFuncSeparate(src, dst, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
			ci.cancel();
		}
	}

}
