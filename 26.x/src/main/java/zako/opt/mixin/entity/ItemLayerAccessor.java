package zako.opt.mixin.entity;

import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerAccessor {
	@Accessor("extents")
	Supplier<Vector3fc[]> zakoopt$extents();

	@Accessor("itemTransform")
	ItemTransform zakoopt$itemTransform();

	@Accessor("localTransform")
	Matrix4f zakoopt$localTransform();
}
