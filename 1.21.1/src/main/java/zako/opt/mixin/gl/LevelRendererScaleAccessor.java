package zako.opt.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererScaleAccessor {
	@Accessor("entityTarget")
	RenderTarget zakoopt$entityOutlineTarget();
}
