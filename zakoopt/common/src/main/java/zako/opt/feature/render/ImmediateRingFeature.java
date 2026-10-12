package zako.opt.feature.render;

import zako.opt.ZakoOptConfig;
import zako.opt.bench.BenchView;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Impact;
import zako.opt.feature.Setting;
import zako.opt.gl.RingAutoTune;

// on its own the ring stays only if the auto-tune measured it faster; "always" skips that measurement
@FeatureData(
	key = "immediate_ring",
	group = FeatureGroup.RENDER,
	impact = Impact.MEDIUM,
	mixins = {"gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"},
	bench = BenchView.OVERVIEW
)
public class ImmediateRingFeature extends Feature {
	@Setting(key = "immediate_ring_force")
	public boolean force = false;

	public boolean forced() {
		return System.getProperty(property()) != null || force;
	}

	@Override
	public void refresh() {
		boolean forced = forced();
		if (Features.active && value && !forced && ZakoOptConfig.RING_PRESENT) {
			RingAutoTune.frame();
		}
		on = flag(value) && (forced || RingAutoTune.ringAllowed());
	}
}
