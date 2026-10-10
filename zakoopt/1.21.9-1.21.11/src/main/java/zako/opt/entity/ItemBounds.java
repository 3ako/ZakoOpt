package zako.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.phys.AABB;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

// model bounds of single-layer items, shared by model: render states are rebuilt every frame and their own cache dies
// with them, so a pile of dropped items recomputed every bound from its quads every frame
@UtilityClass
public class ItemBounds {
	private final Map<Object, Entry> BOUNDS = new IdentityHashMap<>();

	private record Entry(ItemTransform transform, boolean leftHand, AABB bounds) {
	}

	public AABB get(Object extents, ItemTransform transform, boolean leftHand, Supplier<AABB> compute) {
		Entry e = BOUNDS.get(extents);
		if (e != null && e.transform == transform && e.leftHand == leftHand) {
			return e.bounds;
		}
		AABB bounds = compute.get();
		// ponytail: wholesale clear instead of LRU; models only change on resource reload
		if (BOUNDS.size() > 4096) {
			BOUNDS.clear();
		}
		BOUNDS.put(extents, new Entry(transform, leftHand, bounds));
		return bounds;
	}
}
