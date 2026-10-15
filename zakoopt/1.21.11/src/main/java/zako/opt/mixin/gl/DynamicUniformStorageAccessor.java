package zako.opt.mixin.gl;

import net.minecraft.client.renderer.DynamicUniformStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicUniformStorage.class)
public interface DynamicUniformStorageAccessor {
	@Accessor("lastUniform")
	DynamicUniformStorage.DynamicUniform zakoopt$lastUniform();
}
