package zako.opt.mixin.block;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface CreateFrustumAccessor {
	@Accessor("cullingFrustum")
	Frustum zakoopt$frustum();
}
