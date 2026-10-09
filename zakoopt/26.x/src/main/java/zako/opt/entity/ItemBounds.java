package zako.opt.entity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

// model bounds of single-layer items, shared by model: render states are rebuilt every frame and their own cache dies with them
@UtilityClass
public class ItemBounds {
	private final Map<Supplier<Vector3fc[]>, Entry> BOUNDS = new IdentityHashMap<>();

	private record Entry(ItemTransform transform, AABB bounds) {
	}

	public AABB get(Supplier<Vector3fc[]> extents, ItemTransform transform, Supplier<AABB> compute) {
		Entry e = BOUNDS.get(extents);
		if (e != null && e.transform == transform) {
			return e.bounds;
		}
		AABB bounds = compute.get();
		// ponytail: wholesale clear instead of LRU; models only change on resource reload
		if (BOUNDS.size() > 4096) {
			BOUNDS.clear();
		}
		BOUNDS.put(extents, new Entry(transform, bounds));
		return bounds;
	}
}
