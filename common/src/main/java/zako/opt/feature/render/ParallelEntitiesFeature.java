package zako.opt.feature.render;

import zako.opt.MixinPlugin;
import zako.opt.ZakoOptConfig;
import zako.opt.Platform;
import zako.opt.entity.CullingFrustum;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;

// render states of the visible entities are built on the helpers instead of one after another on the render thread
@FeatureData(
	key = "parallel_entities",
	group = FeatureGroup.RENDER,
	mixins = {"render.LevelRendererEntityExtractMixin"}
)
public class ParallelEntitiesFeature extends Feature {
	@Override
	public String blocked() {
		if (ZakoOptConfig.CORES < 4) {
			return "zakoopt.blocked.cpu";
		}
		if (MixinPlugin.VULKAN) {
			return "VulkanMod";
		}
		// both replace entity renderers and keep state of their own around extraction
		if (Platform.isModLoaded("entity_model_features")) {
			return "Entity Model Features";
		}
		if (Platform.isModLoaded("entity_texture_features")) {
			return "Entity Texture Features";
		}
		// its culling worker reads a frustum field the vanilla loop would have set; without it entities are culled wrongly
		if (!CullingFrustum.ready()) {
			return "EntityCulling";
		}
		// the shadow pass extracts every entity a second time for its own camera
		return Features.shaders() ? "Iris" : null;
	}
}
