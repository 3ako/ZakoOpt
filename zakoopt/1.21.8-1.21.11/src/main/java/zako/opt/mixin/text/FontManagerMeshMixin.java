package zako.opt.mixin.text;

import net.minecraft.client.Options;
import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.TextMeshCache;

// recorded text meshes hold glyph positions and atlas coordinates: a font reload or a font option change drops them
@Mixin(FontManager.class)
public class FontManagerMeshMixin {
	@Inject(method = "apply", at = @At("TAIL"))
	private void zakoopt$clearMeshesOnReload(CallbackInfo ci) {
		TextMeshCache.clear();
	}

	@Inject(method = "updateOptions", at = @At("TAIL"))
	private void zakoopt$clearMeshesOnOptions(Options options, CallbackInfo ci) {
		TextMeshCache.clear();
	}
}
