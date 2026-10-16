package zako.opt.bench;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import zako.opt.Canvas;
import zako.opt.ZakoKey;
import zako.opt.ZakoOptConfig;
import zako.opt.update.UpdateUi;

// the tuning panel's live readout over the player's own world, single player or a server; all of it stays on the client
public class MonitorScreen extends ProgressScreen {
	public MonitorScreen() {
		Bench.resetFrames();
		MemoryGraph.reset();
	}

	// a key click only counts while no screen is open
	public static void pollKey() {
		while (ZakoKey.MONITOR.consumeClick()) {
			if (Minecraft.getInstance().level != null && !Bench.running()) {
				UpdateUi.setScreen(new MonitorScreen());
			}
		}
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
		return Component.translatable("zakoopt.monitor.hint");
	}

	@Override
	protected void paint(Canvas c) {
		super.paint(c);
		int w = 240, x0 = width / 2 - w / 2, y0 = height - 8 - 30;
		frame(c, x0, y0, x0 + w, y0 + 30);
		modToggle(c, x0 + 8, y0 + 8, w - 16);
	}

	@Override
	protected void closed() {
		ZakoOptConfig.save();
	}
}
