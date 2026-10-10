package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.ZakoOptConfig;
import zako.opt.block.PistonShape;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityShapeMixin {
	@Shadow
	@Final
	private static ThreadLocal<Direction> NOCLIP;
	@Shadow
	private float progress;
	// one immutable record, so chunk builder threads asking at the same time never see half an update
	@Unique
	private volatile PistonShape zakoopt$shape;

	// the moving block's shape is merged anew on every call (neighbour lighting checks ask many times a frame); it only
	// changes with the progress, which moves once a tick
	@WrapMethod(method = "getCollisionShape")
	private VoxelShape zakoopt$cachedShape(BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(level, pos);
		}
		Direction noclip = NOCLIP.get();
		PistonShape cached = zakoopt$shape;
		if (cached != null && cached.progress() == progress && cached.noclip() == noclip && cached.level() == level && cached.pos().equals(pos)) {
			return cached.shape();
		}
		float at = progress;
		VoxelShape shape = original.call(level, pos);
		zakoopt$shape = new PistonShape(at, noclip, level, pos.immutable(), shape);
		return shape;
	}
}
