package zako.opt.hud;

import com.mojang.blaze3d.platform.Monitor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;

// nanoseconds per refresh of the monitor the window is on
@UtilityClass
@Slf4j(topic = "zakoopt")
public class MonitorRate {
	private long intervalNs = 1_000_000_000L / 144;
	private long nextCheck;

	public long intervalNs(long now) {
		if (now >= nextCheck) {
			// GLFW query, so only once a second; the window may have moved to another monitor
			Monitor monitor = Minecraft.getInstance().getWindow().findBestMonitor();
			int hz = monitor != null ? monitor.getCurrentMode().getRefreshRate() : 144;
			long interval = 1_000_000_000L / Math.max(30, hz);
			if (interval != intervalNs) {
				log.info("Monitor refresh rate {} Hz", hz);
			}
			intervalNs = interval;
			nextCheck = now + 1_000_000_000L;
		}
		return intervalNs;
	}
}
