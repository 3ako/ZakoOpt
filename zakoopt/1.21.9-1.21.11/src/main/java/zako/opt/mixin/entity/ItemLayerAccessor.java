package zako.opt.mixin.entity;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerAccessor {
	@Accessor("extents")
	Supplier<?> zakoopt$extents();

	@Accessor("transform")
	ItemTransform zakoopt$transform();
}
