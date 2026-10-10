package zako.opt.mixin.entity;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {
	@Accessor("cubes")
	List<ModelPart.Cube> zakoopt$cubes();

	@Accessor("children")
	Map<String, ModelPart> zakoopt$children();
}
