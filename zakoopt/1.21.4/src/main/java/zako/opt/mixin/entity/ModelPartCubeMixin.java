package zako.opt.mixin.entity;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.FastCubes;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeMixin implements FastCubes.Holder {
	@Unique
	private FastCubes.Baked zakoopt$baked;

	@Override
	public FastCubes.Baked zakoopt$baked() {
		return zakoopt$baked;
	}

	@Override
	public void zakoopt$baked(FastCubes.Baked baked) {
		zakoopt$baked = baked;
	}
}
