package zako.opt.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.text.TextMeshCache;

@Mixin(Font.class)
public class FontMixin {
	@Shadow
	private int drawInternal(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose,
							 MultiBufferSource buffers, Font.DisplayMode mode, int background, int light, boolean inverseDepth) {
		throw new AssertionError();
	}

	@Inject(method = "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
			at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedMesh(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
									Font.DisplayMode mode, int background, int light, boolean inverseDepth, CallbackInfoReturnable<Integer> cir) {
		if (!TextMeshCache.applies(text)) {
			return;
		}
		Integer width = TextMeshCache.draw(new TextMeshCache.Key(text, x, y, color, shadow, mode, background, inverseDepth), pose, buffers, light,
				recorder -> drawInternal(text, x, y, color, shadow, TextMeshCache.identity(), recorder, mode, background, 0, inverseDepth));
		if (width != null) {
			cir.setReturnValue(width);
		}
	}
}
