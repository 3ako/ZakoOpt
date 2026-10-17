package remodik.opt.feature.render;

import remodik.opt.bench.BenchView;
import remodik.opt.cpu.CorePin;
import remodik.opt.feature.Feature;
import remodik.opt.feature.FeatureData;
import remodik.opt.feature.FeatureGroup;

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
