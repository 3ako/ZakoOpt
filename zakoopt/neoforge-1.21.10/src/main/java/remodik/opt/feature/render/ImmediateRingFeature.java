package remodik.opt.feature.render;

import remodik.opt.ZakoOptConfig;
import remodik.opt.bench.BenchView;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;
import remodik.opt.feature.Features;
import remodik.opt.feature.Setting;
import remodik.opt.gl.RingAutoTune;

// on its own the ring stays only if the auto-tune measured it faster; "always" skips that measurement
@FeatureData(
	key = "immediate_ring",
	group = FeatureGroup.RENDER,
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
