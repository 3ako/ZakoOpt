package zako.opt.mixin.entity;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.GuiEntities;

@Mixin(GuiRenderer.class)
public class GuiRendererEntityMixin {
	@Inject(method = "preparePictureInPicture", at = @At("HEAD"))
	private void zakoopt$guiStart(CallbackInfo ci) {
		GuiEntities.depth++;
	}

	@Inject(method = "preparePictureInPicture", at = @At("RETURN"))
	private void zakoopt$guiEnd(CallbackInfo ci) {
		GuiEntities.depth--;
	}
}
