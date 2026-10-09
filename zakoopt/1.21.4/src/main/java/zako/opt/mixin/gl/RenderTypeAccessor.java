package zako.opt.mixin.gl;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface RenderTypeAccessor {
	@Accessor("name")
	String zakoopt$name();
}
