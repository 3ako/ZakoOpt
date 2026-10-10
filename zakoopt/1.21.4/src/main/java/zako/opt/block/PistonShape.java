package zako.opt.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.VoxelShape;

public record PistonShape(float progress, Direction noclip, BlockGetter level, BlockPos pos, VoxelShape shape) {
}
