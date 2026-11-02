package zako.opt.feature.blocks;

import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;

// vanilla objects made anew every frame are reused instead; the Iris shadow pass extracts the same block entities again
// for its own camera, which would overwrite a reused state or pose
@FeatureData(
	key = "fewer_allocations",
	group = FeatureGroup.MEMORY,
	mixins = {"block.BlockEntityRenderStatePoolMixin", "entity.SubmitPosePoolMixin", "gl.BatchableBufferSourceSetPoolMixin", "entity.ModelPartChildrenMixin"}
)
public class FewerAllocationsFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
