package zako.opt.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererScaleAccessor {
	@Accessor("hud3DTarget")
	RenderTarget zakoopt$hud3DTarget();
}
