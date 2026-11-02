package zako.opt.bench;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import zako.opt.ZakoKey;
import zako.opt.ZakoOptConfig;

// the tuning panel's live readout over the player's own world, single player or a server; all of it stays on the client.
// It is drawn over the HUD like F3, never opened as a screen, so the player keeps moving with it up
public class MonitorScreen extends ProgressScreen {
	private static MonitorScreen shown;

	// the key shows or hides the panel, Shift with it switches the mod; a key click only counts while no screen is open
	public static void pollKey() {
		Minecraft mc = Minecraft.getInstance();
		while (ZakoKey.MONITOR.consumeClick()) {
			if (ZakoKey.shift()) {
				ZakoOptConfig.enabled = !ZakoOptConfig.enabled;
				ZakoOptConfig.save();
			} else if (shown != null) {
				shown = null;
			} else if (mc.level != null && !Bench.running()) {
				show();
			}
		}
		if (shown != null && (mc.level == null || Bench.running())) {
			shown = null;
		}
	}

	public static void show() {
		Bench.resetFrames();
		MemoryGraph.reset();
		shown = new MonitorScreen();
	}

	public static MonitorScreen shown() {
		return shown;
	}

	@Override
	protected Component stage() {
		return Component.translatable(ZakoOptConfig.enabled ? "zakoopt.bench.compare.on" : "zakoopt.bench.compare.off");
	}

	@Override
	protected int percent() {
		return -1;
	}

	@Override
	protected Component hint() {
		Component key = ZakoKey.MONITOR.getTranslatedKeyMessage();
		return Component.translatable("zakoopt.monitor.hint", key, key);
	}
}
