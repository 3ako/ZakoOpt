package zako.opt.block;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

@UtilityClass
public class LeafCulling {
	// a leaf face toward another leaf goes only when a leaf or a solid block stands behind that one too: the canopy keeps its
	// outer layer, seen through its holes, and only its inside goes hollow. The slice reaches two blocks past its section
	public boolean hidden(BlockState self, LevelSlice slice, BlockPos pos, Direction dir) {
		if (!(self.getBlock() instanceof LeavesBlock)) {
			return false;
		}
		int x = pos.getX() + dir.getStepX(), y = pos.getY() + dir.getStepY(), z = pos.getZ() + dir.getStepZ();
		if (!(slice.getBlockState(x, y, z).getBlock() instanceof LeavesBlock)) {
			return false;
		}
		BlockPos far = new BlockPos(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
		BlockState beyond = slice.getBlockState(far);
		return beyond.getBlock() instanceof LeavesBlock || beyond.isSolidRender(slice, far);
	}
}
