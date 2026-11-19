package zako.opt.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// other mods (EntityCulling) inject into this private method; going through it keeps their culling in the parallel path
@Mixin(LevelRenderer.class)
public interface LevelRendererExtractInvoker {
	@Invoker("extractEntity")
	EntityRenderState zakoopt$extractEntity(Entity entity, float partialTick);
}
