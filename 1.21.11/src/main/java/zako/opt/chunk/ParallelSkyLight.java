package zako.opt.chunk;

import zako.opt.feature.Features;
import zako.opt.particle.Workers;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import zako.opt.mixin.chunk.ChunkSkyLightSourcesAccessor;

// fillFrom scans 256 columns of a chunk on the main thread while the chunk applies. The columns only read block states,
// so they run on the workers and the heightmap is written afterwards on the main thread. Only the client thread may
// start it: server chunk workers calling back into the same pool would be able to deadlock it.
@UtilityClass
public class ParallelSkyLight {
	private final int ROWS = 8;
	private final int COLUMNS_PER_ROW = 256 / ROWS;
	private final ThreadLocal<BlockPos.MutableBlockPos[]> POSITIONS =
		ThreadLocal.withInitial(() -> new BlockPos.MutableBlockPos[]{new BlockPos.MutableBlockPos(), new BlockPos.MutableBlockPos()});

	public boolean fill(ChunkAccess chunk, ChunkSkyLightSources target) {
		if (!Features.FAST_LIGHT.on || !Minecraft.getInstance().isSameThread()) {
			return false;
		}
		ChunkSkyLightSourcesAccessor accessor = (ChunkSkyLightSourcesAccessor) target;
		BitStorage heightmap = accessor.zakoopt$heightmap();
		int minY = accessor.zakoopt$minY();
		int highest = chunk.getHighestFilledSectionIndex();
		if (highest == -1) {
			// vanilla fills every column with minY, which is 0 in the heightmap's own coordinates
			for (int i = 0; i < 256; i++) {
				heightmap.set(i, 0);
			}
			return true;
		}
		int[] result = new int[256];
		Throwable error = Workers.run(ROWS, row -> {
			BlockPos.MutableBlockPos[] positions = POSITIONS.get();
			int from = row * COLUMNS_PER_ROW;
			for (int i = from; i < from + COLUMNS_PER_ROW; i++) {
				int x = i & 15;
				int z = i >> 4;
				result[i] = Math.max(findLowestSourceY(chunk, highest, x, z, positions[0], positions[1]), minY);
			}
		});
		if (error != null) {
			// let the vanilla loop run instead of applying a half-scanned chunk
			return false;
		}
		for (int i = 0; i < 256; i++) {
			heightmap.set(i, result[i] - minY);
		}
		return true;
	}

	// the exact walk of ChunkSkyLightSources.findLowestSourceY, with per-thread positions instead of the shared fields
	private int findLowestSourceY(ChunkAccess chunk, int highest, int x, int z, BlockPos.MutableBlockPos pos1, BlockPos.MutableBlockPos pos2) {
		int y = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(highest) + 1);
		pos1.set(x, y, z);
		pos2.setWithOffset(pos1, Direction.DOWN);
		BlockState state = Blocks.AIR.defaultBlockState();

		for (int sectionIndex = highest; sectionIndex >= 0; sectionIndex--) {
			LevelChunkSection section = chunk.getSection(sectionIndex);
			if (section.hasOnlyAir()) {
				state = Blocks.AIR.defaultBlockState();
				int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
				pos1.setY(SectionPos.sectionToBlockCoord(sectionY));
				pos2.setY(pos1.getY() - 1);
			} else {
				for (int localY = 15; localY >= 0; localY--) {
					BlockState above = section.getBlockState(x, localY, z);
					if (isEdgeOccluded(state, above)) {
						return pos1.getY();
					}
					state = above;
					pos1.set(pos2);
					pos2.move(Direction.DOWN);
				}
			}
		}

		return Integer.MIN_VALUE;
	}

	private boolean isEdgeOccluded(BlockState below, BlockState above) {
		if (above.getLightBlock() != 0) {
			return true;
		}
		VoxelShape belowShape = LightEngine.getOcclusionShape(below, Direction.DOWN);
		VoxelShape aboveShape = LightEngine.getOcclusionShape(above, Direction.UP);
		return Shapes.faceShapeOccludes(belowShape, aboveShape);
	}
}
