package zako.opt.block;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Create draws items its renderers hold (value box filters, items riding belts) through the whole item pipeline every frame;
// each is drawn once in its own space and replayed through its pose while the item, its model, the light and the caller stay the same
@UtilityClass
public class ItemMeshes {
	// by what the stack holds, not by the stack object: every block entity packet from the server decodes new stacks
	private final Map<Key, Entry> CACHE = new HashMap<>();
	// Create's classes no longer match ours: the original draws from then on
	private boolean off;
	public boolean recording;

	// the count is left out: an item draws the same however many there are
	private record Key(Object kind, ItemStack stack, int light, int overlay) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.kind == kind && k.light == light && k.overlay == overlay && ItemStack.isSameItemSameComponents(k.stack, stack);
		}

		@Override
		public int hashCode() {
			return (31 * System.identityHashCode(kind) + ItemStack.hashItemAndComponents(stack)) * 31 + light * 7 + overlay;
		}
	}

	private static final class Entry {
		BakedModel model;
		List<SpawnerMeshCache.Segment> mesh;
	}

	// kind tells the callers apart, as each records a different part of the drawing; false: draw it the original way
	public boolean render(Object kind, ItemStack stack, BakedModel model, PoseStack ms, MultiBufferSource buffer, int light, int overlay, Consumer<MultiBufferSource> draw) {
		// a glint goes to two buffers at once, which the recorder keeps apart; custom renderers may animate
		if (off || stack.isEmpty() || model.isCustomRenderer() || stack.hasFoil()) {
			return false;
		}
		try {
			Entry e = CACHE.get(new Key(kind, stack, light, overlay));
			if (e == null) {
				// ponytail: wholesale clear instead of LRU, the items in view refill it within a frame
				if (CACHE.size() >= 8192) {
					CACHE.values().stream().filter(v -> v.mesh != null).forEach(v -> SpawnerMeshCache.free(v.mesh));
					CACHE.clear();
				}
				e = new Entry();
				// the key keeps its own copy: the stack it was made from may change in place
				CACHE.put(new Key(kind, stack.copyWithCount(1), light, overlay), e);
			}
			if (e.mesh == null || e.model != model) {
				if (e.mesh != null) {
					SpawnerMeshCache.free(e.mesh);
					e.mesh = null;
				}
				e.model = model;
				recording = true;
				try {
					e.mesh = SpawnerMeshCache.record(draw, true);
				} finally {
					recording = false;
				}
			}
			return SpawnerMeshCache.emit(e.mesh, ms.last(), buffer, light);
		} catch (RuntimeException | LinkageError t) {
			off = true;
			throw t;
		}
	}
}
