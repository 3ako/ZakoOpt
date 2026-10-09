package zako.opt.mixin.hud;

import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiItemAtlas.class)
public interface GuiItemAtlasAccessor {
	@Accessor("allocator")
	DynamicAtlasAllocator<Object> zakoopt$allocator();
}
