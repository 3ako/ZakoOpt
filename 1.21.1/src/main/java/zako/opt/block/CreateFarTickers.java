package zako.opt.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.phys.Vec3;
import zako.opt.feature.Features;
import zako.opt.mixin.block.CreateBoundTickerAccessor;
import zako.opt.mixin.block.CreateRebindableTickerAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// the client level walks every ticking block entity each tick, even those whose tick far-machine rest skips: in a factory district
// that walk was the longest part of a tick frame. Far Create tickers leave the level's list for a parked one and come back on approach
@UtilityClass
public class CreateFarTickers {
	private final Map<Level, List<TickingBlockEntity>> PARKED = new WeakHashMap<>();
	// a slice of each list a tick, a full round a second: a whole pass at once was itself a spike in the tick frame
	private final int ROUND = 20;
	private int cursor, parkedCursor;

	public void sweep(Level level, List<TickingBlockEntity> tickers) {
		List<TickingBlockEntity> parked = PARKED.computeIfAbsent(level, l -> new ArrayList<>());
		if (!Features.CREATE_FAR_TICK.on) {
			if (!parked.isEmpty()) {
				tickers.addAll(parked);
				parked.clear();
			}
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		double far = Features.CREATE_FAR_TICK.distance;
		double view = mc.options.getEffectiveRenderDistance() * 16 + 32;
		List<TickingBlockEntity> woken = new ArrayList<>();
		parkedCursor = slice(parked, parkedCursor, t -> t.isRemoved() || !far(t.getPos(), cam, far, view) && woken.add(t));
		cursor = slice(tickers, cursor, t -> !t.isRemoved() && create(t) && far(t.getPos(), cam, far, view) && parked.add(t));
		tickers.addAll(woken);
	}

	// removeIf on a sublist shifts the tail once for the whole slice; returns where the next slice starts
	private int slice(List<TickingBlockEntity> list, int from, java.util.function.Predicate<TickingBlockEntity> take) {
		int size = list.size();
		if (from >= size) {
			from = 0;
		}
		int to = Math.min(size, from + Math.max(32, size / ROUND));
		List<TickingBlockEntity> part = list.subList(from, to);
		int before = part.size();
		part.removeIf(take);
		return to - (before - part.size());
	}

	// Sable keeps sub-level blocks in plots far past the view distance: those stay in the list
	private boolean far(BlockPos pos, Vec3 cam, double far, double view) {
		double dx = pos.getX() + 0.5 - cam.x, dy = pos.getY() + 0.5 - cam.y, dz = pos.getZ() + 0.5 - cam.z;
		return dx * dx + dy * dy + dz * dz > far * far && dx * dx + dz * dz < view * view;
	}

	private boolean create(TickingBlockEntity t) {
		if (t instanceof CreateRebindableTickerAccessor wrapper) {
			t = wrapper.zakoopt$ticker();
		}
		return t instanceof CreateBoundTickerAccessor bound && bound.zakoopt$ticker() instanceof SmartBlockEntityTicker;
	}
}
