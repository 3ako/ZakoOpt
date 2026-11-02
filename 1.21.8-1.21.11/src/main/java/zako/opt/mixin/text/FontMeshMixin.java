package zako.opt.mixin.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.text.TextMeshCache;

// both world-text entry points: the Component one (name tags) is the sequence one on getVisualOrderText(), so both replay
// the same mesh and record through the sequence one
@Mixin(Font.class)
public abstract class FontMeshMixin {
	@Shadow
	public abstract void drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
									 Font.DisplayMode mode, int background, int light);

	@Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
			at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedMesh(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
									Font.DisplayMode mode, int background, int light, CallbackInfo ci) {
		if (zakoopt$draw(text, x, y, color, shadow, pose, buffers, mode, background, light)) {
			ci.cancel();
		}
	}

	@Inject(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
			at = @At("HEAD"), cancellable = true)
	private void zakoopt$cachedComponentMesh(Component text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
											 Font.DisplayMode mode, int background, int light, CallbackInfo ci) {
		if (zakoopt$draw(text.getVisualOrderText(), x, y, color, shadow, pose, buffers, mode, background, light)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean zakoopt$draw(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose, MultiBufferSource buffers,
								 Font.DisplayMode mode, int background, int light) {
		if (!TextMeshCache.applies(text)) {
			return false;
		}
		return TextMeshCache.draw(new TextMeshCache.Key(text, x, y, color, shadow, mode, background), pose, buffers, light,
				recorder -> drawInBatch(text, x, y, color, shadow, TextMeshCache.identity(), recorder, mode, background, 0));
	}
}
