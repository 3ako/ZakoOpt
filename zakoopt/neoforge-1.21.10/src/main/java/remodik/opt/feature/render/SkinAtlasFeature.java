package remodik.opt.feature.render;

import remodik.opt.MixinPlugin;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

@FeatureData(
	key = "skin_atlas",
	group = FeatureGroup.RENDER,
	mixins = "entity.LivingEntityRendererSkinMixin"
)
public class SkinAtlasFeature extends Feature {
	@Override
	public String blocked() {
		return MixinPlugin.VULKAN ? "VulkanMod" : null;
	}
}
