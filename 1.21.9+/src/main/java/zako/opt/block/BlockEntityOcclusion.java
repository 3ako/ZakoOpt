package zako.opt.block;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;

// a block entity behind opaque blocks is skipped. Hidden holds only for the exact camera position it was traced from, so
// nothing pops in late: a moved camera re-traces it, and past the frame's budget it is simply drawn
@UtilityClass
public class BlockEntityOcclusion {
	private final int BUDGET = 3000;
	private final int MAX_STEPS = 256;
	private Vec3 frame;
	private int budget;

	private record Traced(double x, double y, double z, long tick, boolean hidden) {
	}

	public boolean hidden(BlockEntity blockEntity, Vec3 camera) {
		Level level = blockEntity.getLevel();
		if (level == null || !fitsItsBlock(blockEntity)) {
			return false;
		}
		if (camera != frame) {
			frame = camera;
			budget = BUDGET;
		}
		OcclusionSlot slot = (OcclusionSlot) blockEntity;
		long tick = level.getGameTime();
		if (slot.zakoopt$occlusion() instanceof Traced t) {
			boolean same = t.x == camera.x && t.y == camera.y && t.z == camera.z;
			// a visible one is re-traced once a second: that only decides how soon it starts being skipped
			if (t.hidden ? same : tick - t.tick < 20) {
				return t.hidden;
			}
		}
		if (budget <= 0) {
			return false;
		}
		budget--;
		boolean hidden = !visible(level, camera, blockEntity.getBlockPos(), blockEntity instanceof BannerBlockEntity ? 2.1 : 1.6);
		slot.zakoopt$occlusion(new Traced(camera.x, camera.y, camera.z, tick, hidden));
		return hidden;
	}

	// models that stay inside their block, a little over it (shulker lid, banner cloth) covered by the box margins
	private boolean fitsItsBlock(BlockEntity blockEntity) {
		return blockEntity instanceof SignBlockEntity || blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity
				|| blockEntity instanceof ShulkerBoxBlockEntity || blockEntity instanceof SkullBlockEntity || blockEntity instanceof DecoratedPotBlockEntity
				|| blockEntity instanceof CampfireBlockEntity || blockEntity instanceof BannerBlockEntity;
	}

	// corners of a box a bit larger than the block, and its centre; a corner inside a neighbouring wall counts as reached
	private boolean visible(Level level, Vec3 camera, BlockPos pos, double top) {
		double x0 = pos.getX() - 0.1, y0 = pos.getY() - 0.1, z0 = pos.getZ() - 0.1;
		double x1 = pos.getX() + 1.1, y1 = pos.getY() + top, z1 = pos.getZ() + 1.1;
		if (ray(level, camera, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			return true;
		}
		for (int i = 0; i < 8; i++) {
			if (ray(level, camera, (i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1)) {
				return true;
			}
		}
		return false;
	}

	// voxel walk (Amanatides-Woo) from the camera to the point; the camera's and the point's own voxels are not tested
	private boolean ray(Level level, Vec3 camera, double tx, double ty, double tz) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		double dx = tx - camera.x, dy = ty - camera.y, dz = tz - camera.z;
		int x = floor(camera.x), y = floor(camera.y), z = floor(camera.z);
		int ex = floor(tx), ey = floor(ty), ez = floor(tz);
		int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
		double nx = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
		double ny = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
		double nz = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
		double mx = first(dx, camera.x, x), my = first(dy, camera.y, y), mz = first(dz, camera.z, z);
		// t runs from 0 at the camera to 1 at the point: past 1 the walk would test voxels behind the point
		for (int step = 0; step < MAX_STEPS && Math.min(mx, Math.min(my, mz)) < 1; step++) {
			if (mx < my && mx < mz) {
				x += sx;
				mx += nx;
			} else if (my < mz) {
				y += sy;
				my += ny;
			} else {
				z += sz;
				mz += nz;
			}
			if (x == ex && y == ey && z == ez) {
				return true;
			}
			if (level.getBlockState(cursor.set(x, y, z)).isSolidRender()) {
				return false;
			}
		}
		return true;
	}

	// t of the first voxel boundary along one axis; an axis the ray does not move along never crosses one
	private double first(double d, double from, int voxel) {
		return d == 0 ? Double.POSITIVE_INFINITY : (d > 0 ? voxel + 1 - from : from - voxel) / Math.abs(d);
	}

	private int floor(double v) {
		int i = (int) v;
		return v < i ? i - 1 : i;
	}
}
