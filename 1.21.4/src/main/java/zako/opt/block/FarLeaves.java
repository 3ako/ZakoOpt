package zako.opt.block;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import zako.opt.feature.Features;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// leaves in sections past the distance are meshed solid, as fast graphics does: no see-through holes, so the GPU draws one
// layer of them instead of every layer behind. Which way a section was meshed is remembered, and once the camera has moved it
// past the distance (with some slack, so walking along the edge does not rebuild it back and forth) it is meshed again
@UtilityClass
public class FarLeaves {
	private final int SLACK = 8;
	private final double RECHECK = 4;

	private final Map<Long, Boolean> sections = new ConcurrentHashMap<>();
	private volatile double camX, camY, camZ;
	private double checkedX = Double.NaN, checkedY, checkedZ;
	private int checkedDistance;
	// bumped whenever the camera position the builders measure from moves on, or a section is flipped
	private volatile int epoch;

	// one per builder thread: the last section's verdict. A section is meshed by one thread at a time, so its blocks are
	// decided once instead of per quad
	public final class Section {
		private long key = Long.MIN_VALUE;
		private int epoch = -1;
		private boolean far;
	}

	// on a chunk builder thread
	private boolean far(Section last, BlockPos pos) {
		int sx = pos.getX() >> 4, sy = pos.getY() >> 4, sz = pos.getZ() >> 4;
		long key = SectionPos.asLong(sx, sy, sz);
		int e = epoch;
		if (key != last.key || e != last.epoch) {
			int limit = Features.FAR_LEAVES.distance;
			last.far = distanceSqr(sx, sy, sz) > (double) limit * limit;
			last.key = key;
			last.epoch = e;
			sections.put(key, last.far);
		}
		return last.far;
	}

	// on a chunk builder thread
	public RenderType type(Section last, BlockState state, BlockPos pos, RenderType original) {
		if (!Features.FAR_LEAVES.on || state == null || !(state.getBlock() instanceof LeavesBlock)) {
			return original;
		}
		return far(last, pos) ? RenderType.solid() : original;
	}

	public void frame() {
		if (!Features.FAR_LEAVES.on) {
			if (!sections.isEmpty()) {
				sections.clear();
				epoch++;
			}
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		camX = cam.x;
		camY = cam.y;
		camZ = cam.z;
		double dx = camX - checkedX, dy = camY - checkedY, dz = camZ - checkedZ;
		int limit = Features.FAR_LEAVES.distance;
		if (!(dx * dx + dy * dy + dz * dz >= RECHECK * RECHECK) && limit == checkedDistance) {
			return;
		}
		checkedX = camX;
		checkedY = camY;
		checkedZ = camZ;
		checkedDistance = limit;
		epoch++;
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
		double keep = (mc.options.getEffectiveRenderDistance() + 2) * 16;
		for (Iterator<Map.Entry<Long, Boolean>> it = sections.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Long, Boolean> e = it.next();
			long key = e.getKey();
			int sx = SectionPos.x(key), sy = SectionPos.y(key), sz = SectionPos.z(key);
			double d = distance(sx, sy, sz);
			if (d > keep) {
				it.remove();
			} else if (renderer != null && (e.getValue() ? d < limit - SLACK : d > limit + SLACK)) {
				e.setValue(!e.getValue());
				renderer.scheduleRebuildForChunk(sx, sy, sz, false);
			}
		}
	}

	private double distance(int sx, int sy, int sz) {
		return Math.sqrt(distanceSqr(sx, sy, sz));
	}

	private double distanceSqr(int sx, int sy, int sz) {
		double dx = (sx << 4) + 8 - camX, dy = (sy << 4) + 8 - camY, dz = (sz << 4) + 8 - camZ;
		return dx * dx + dy * dy + dz * dz;
	}
}
