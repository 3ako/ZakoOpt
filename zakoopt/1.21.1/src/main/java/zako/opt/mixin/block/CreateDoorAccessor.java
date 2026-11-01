package zako.opt.mixin.block;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(value = SlidingDoorBlockEntity.class, remap = false)
public interface CreateDoorAccessor {
	@Accessor("animation")
	LerpedFloat zakoopt$animation();

	@Invoker("shouldRenderSpecial")
	boolean zakoopt$shouldRenderSpecial(BlockState state);
}
