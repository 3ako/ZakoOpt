package zako.opt.feature.render;

import zako.opt.bench.BenchView;
import zako.opt.cpu.CorePin;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureData;
import zako.opt.feature.FeatureGroup;

@FeatureData(
	key = "core_pin",
	group = FeatureGroup.RENDER,
	enabled = false,
	bench = BenchView.CROWD
)
public class CorePinFeature extends Feature {
	@Override
	public String blocked() {
		return CorePin.available() ? null : "zakoopt.blocked.cores_same";
	}

	// refresh runs on the render thread, which moves itself here
	@Override
	public void refresh() {
		super.refresh();
		CorePin.set(on);
		CorePin.check();
	}
}
