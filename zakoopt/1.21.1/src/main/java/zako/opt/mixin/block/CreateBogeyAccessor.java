package zako.opt.mixin.block;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = CarriageBogey.class, remap = false)
public interface CreateBogeyAccessor {
	@Accessor("type")
	AbstractBogeyBlock<?> zakoopt$type();
}
