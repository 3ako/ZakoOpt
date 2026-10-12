package zako.opt.feature.render;

import zako.opt.MixinPlugin;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Impact;

@FeatureData(
	key = "skin_atlas",
	group = FeatureGroup.RENDER,
	impact = Impact.MEDIUM,
	mixins = "entity.LivingEntityRendererSkinMixin"
)
public class SkinAtlasFeature extends Feature {
	@Override
	public String blocked() {
		return MixinPlugin.VULKAN ? "VulkanMod" : null;
	}
}
