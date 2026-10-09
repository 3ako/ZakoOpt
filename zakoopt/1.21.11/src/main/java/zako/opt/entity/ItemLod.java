package zako.opt.entity;

import com.google.common.collect.MapMaker;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Map;
import zako.opt.ZakoOptConfig;

@UtilityClass
public class ItemLod {
	private final float EDGE = 1f / 16f + 1e-4f;
	// keyed by the model's first quad: the quad list passed to renderItem is a reused per-render-state buffer
	private final Map<BakedQuad, Flat> FLAT = new MapMaker().weakKeys().makeMap();

	public boolean enabled() {
		return ZakoOptConfig.itemLod();
	}

	public float distanceSq() {
		float d = ZakoOptConfig.itemLodDistance();
		return d * d;
	}

	// generated (flat) item = two big N/S faces + edge strips only 1/16 deep in z; anything else is returned unchanged
	public List<BakedQuad> flat(List<BakedQuad> quads) {
		if (quads.isEmpty()) {
			return quads;
		}
		Flat cached = FLAT.get(quads.getFirst());
		if (cached == null || cached.sourceSize != quads.size()) {
			cached = new Flat(quads.size(), compute(quads));
			FLAT.put(quads.getFirst(), cached);
		}
		return cached.quads == null ? quads : cached.quads;
	}

	private record Flat(int sourceSize, List<BakedQuad> quads) {
	}

	private List<BakedQuad> compute(List<BakedQuad> quads) {
		if (quads.size() < 8) {
			return null;
		}
		for (BakedQuad q : quads) {
			if (q.direction().getAxis() != Direction.Axis.Z && !thin(q)) {
				return null;
			}
		}
		List<BakedQuad> faces = List.copyOf(quads.stream().filter(q -> q.direction().getAxis() == Direction.Axis.Z).toList());
		return faces.isEmpty() ? null : faces;
	}

	private boolean thin(BakedQuad q) {
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			Vector3fc p = q.position(i);
			minZ = Math.min(minZ, p.z());
			maxZ = Math.max(maxZ, p.z());
		}
		return maxZ - minZ <= EDGE;
	}
}
