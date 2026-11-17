package zako.opt.entity;

import zako.opt.feature.Features;

import com.google.common.collect.MapMaker;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;
import org.joml.Matrix4f;
import java.util.Map;

@UtilityClass
public class ItemLod {
	private final float EDGE = 1f / 16f + 1e-4f;
	// keyed by the model's first quad: the quad list passed to renderItem is a reused per-render-state buffer
	private final Map<BakedQuad, Flat> FLAT = new MapMaker().weakKeys().makeMap();

	public boolean applies(ItemDisplayContext context) {
		return Features.ITEM_LOD.on && !context.firstPerson() && context != ItemDisplayContext.GUI
				&& (Features.ITEM_LOD.held || context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
	}

	public boolean far(Matrix4f m) {
		if (GuiEntities.depth > 0) {
			return false;
		}
		float d = Features.ITEM_LOD.distance;
		return m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32() > d * d;
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
			if (Quads.direction(q).getAxis() != Direction.Axis.Z && !thin(q)) {
				return null;
			}
		}
		List<BakedQuad> faces = quads.stream().filter(q -> Quads.direction(q).getAxis() == Direction.Axis.Z).toList();
		return faces.isEmpty() ? null : faces;
	}

	private boolean thin(BakedQuad q) {
		float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			float z = Quads.z(q, i);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
		}
		return maxZ - minZ <= EDGE;
	}
}
