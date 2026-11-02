package zako.opt.bench;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Idle {
	// 1.21.1 has no idle frame cap
	public void keepAwake() {
	}
}
