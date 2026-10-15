package zako.opt.feature.blocks;

import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Impact;

// vanilla objects made anew every frame are reused instead; the Iris shadow pass extracts the same block entities again
// for its own camera, which would overwrite a reused state
@FeatureData(
	key = "fewer_allocations",
	group = FeatureGroup.BLOCKS,
	impact = Impact.LOW,
	mixins = "block.BlockEntityRenderStatePoolMixin",
	bench = BenchView.OVERVIEW
)
public class FewerAllocationsFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
