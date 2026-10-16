package zako.opt.entity;

import zako.opt.feature.Features;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

// every submit keeps its own copy of the pose until the features are drawn at the end of the frame: the copies come from
// a pool handed out again from the start of every frame, when the last frame's submits are all drawn. A mod that keeps a
// submit or its pose() past the frame would see it overwritten; Iris's shadow pass turns the feature off
@UtilityClass
public class PosePool {
	private final List<PoseStack.Pose> POOL = new ArrayList<>();
	private int used;
	private Thread renderThread;

	// start of a frame, on the render thread
	public void reset() {
		used = 0;
		renderThread = Thread.currentThread();
	}

	// submits made off the render thread copy the vanilla way
	public boolean pooling() {
		return Features.FEWER_ALLOCATIONS.on && Thread.currentThread() == renderThread;
	}

	public PoseStack.Pose copy(PoseStack.Pose pose) {
		if (used == POOL.size()) {
			POOL.add(pose.copy());
			return POOL.get(used++);
		}
		PoseStack.Pose pooled = POOL.get(used++);
		pooled.set(pose);
		return pooled;
	}
}
