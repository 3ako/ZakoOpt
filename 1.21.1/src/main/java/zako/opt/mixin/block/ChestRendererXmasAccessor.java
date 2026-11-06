package zako.opt.mixin.block;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChestRenderer.class)
public interface ChestRendererXmasAccessor {
	@Accessor("xmasTextures")
	boolean zakoopt$xmas();
}
