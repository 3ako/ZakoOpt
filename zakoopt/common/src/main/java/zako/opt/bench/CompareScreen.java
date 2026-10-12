package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.chat.Component;
import zako.opt.Canvas;
import zako.opt.ZakoOptConfig;
import zako.opt.cpu.CorePin;
import zako.opt.feature.Features;

import java.util.concurrent.CompletableFuture;

// the end of the test: the scene stays, and the player flips the whole mod on and off with the picked settings to see the difference
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompareScreen extends ProgressScreen {
	CompletableFuture<Void> done;
	@NonFinal
	int view = Scene.views().indexOf(Scene.OVERVIEW);

	public CompareScreen(CompletableFuture<Void> done) {
		this.done = done;
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
		return Component.translatable("zakoopt.bench.compare.hint");
	}

	@Override
	protected void paint(Canvas c) {
		super.paint(c);
		int w = 240, x0 = width / 2 - w / 2, x1 = x0 + w, y0 = height - 8 - 66, inner = w - 16;
		frame(c, x0, y0, x1, y0 + 66);
		boolean on = ZakoOptConfig.enabled;
		Component toggle = Component.translatable(on ? "zakoopt.bench.compare.toggle.on" : "zakoopt.bench.compare.toggle.off");
		button(c, x0 + 8, y0 + 8, inner, toggle, on ? ON : MASTER_OFF, on ? ACCENT : MASTER_OFF_FRAME, true, () -> ZakoOptConfig.enabled = !ZakoOptConfig.enabled);

		int ry = y0 + 26;
		button(c, x0 + 8, ry, 20, Component.literal("◀"), BUTTON, BUTTON_FRAME, true, () -> move(Scene.views().size() - 1));
		button(c, x1 - 28, ry, 20, Component.literal("▶"), BUTTON, BUTTON_FRAME, true, () -> move(1));
		Component name = Component.translatable("zakoopt.bench.view." + Scene.views().get(view).key());
		c.text(name, x0 + w / 2 - font.width(name) / 2, ry + 3, STRONG);

		ry += 18;
		Component close = Component.translatable("gui.done");
		if (CorePin.available()) {
			int half = (inner - 6) / 2;
			Component cores = Component.translatable("zakoopt.bench.compare.cores",
					Component.translatable(Features.CORE_PIN.value ? "zakoopt.bench.compare.cores.best" : "zakoopt.bench.compare.cores.all"));
			button(c, x0 + 8, ry, half, cores, BUTTON, BUTTON_FRAME, true, () -> Features.CORE_PIN.value = !Features.CORE_PIN.value);
			button(c, x1 - 8 - half, ry, half, close, BUTTON, BUTTON_FRAME, true, this::onClose);
		} else {
			button(c, x0 + 8, ry, inner, close, BUTTON, BUTTON_FRAME, true, this::onClose);
		}
	}

	private void move(int step) {
		view = (view + step) % Scene.views().size();
		Bench.teleport(Scene.views().get(view));
	}

	@Override
	protected void closed() {
		done.complete(null);
	}
}
