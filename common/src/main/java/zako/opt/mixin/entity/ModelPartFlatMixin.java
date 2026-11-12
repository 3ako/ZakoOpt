package zako.opt.mixin.entity;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.FlatSlot;

@Mixin(ModelPart.class)
public class ModelPartFlatMixin implements FlatSlot {
	@Unique
	private Object zakoopt$flat;

	@Override
	public Object zakoopt$flat() {
		return zakoopt$flat;
	}

	@Override
	public void zakoopt$flat(Object flat) {
		zakoopt$flat = flat;
	}
}
