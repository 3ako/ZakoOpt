package zako.opt.mixin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.entity.FlatSlot;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
@Mixin(ModelPart.class)
public class ModelPartFlatMixin implements FlatSlot {
	@Unique
	private Object zakoopt$flat;
}
