package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.hud.HudCache;

@Mixin(GuiRenderer.class)
public class GuiRendererTargetMixin {
	@WrapOperation(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private RenderTarget zakoopt$hudTarget(Minecraft minecraft, Operation<RenderTarget> original) {
		RenderTarget target = HudCache.drawTarget();
		return target != null ? target : original.call(minecraft);
	}
}
