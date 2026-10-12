package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import zako.opt.ZakoOptConfig;
import zako.opt.cpu.CorePin;

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
		boolean on = ZakoOptConfig.values.enabled;
		return Component.translatable(on ? "zakoopt.bench.compare.on" : "zakoopt.bench.compare.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
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
	protected void init() {
		super.init();
		int y = height - 104;
		addRenderableWidget(Button.builder(toggleLabel(), b -> {
			ZakoOptConfig.values.enabled = !ZakoOptConfig.values.enabled;
			b.setMessage(toggleLabel());
		}).bounds(width / 2 - 120, y, 240, 20).build());
		Button name = addRenderableWidget(Button.builder(viewLabel(), b -> {
		}).bounds(width / 2 - 80, y + 28, 160, 20).build());
		name.active = false;
		addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
			view = (view + Scene.views().size() - 1) % Scene.views().size();
			moved(name);
		}).bounds(width / 2 - 104, y + 28, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
			view = (view + 1) % Scene.views().size();
			moved(name);
		}).bounds(width / 2 + 84, y + 28, 20, 20).build());
		if (CorePin.available()) {
			addRenderableWidget(Button.builder(coresLabel(), b -> {
				ZakoOptConfig.values.corePin = !ZakoOptConfig.values.corePin;
				b.setMessage(coresLabel());
			}).bounds(width / 2 - 154, y + 60, 150, 20).build());
			addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 + 4, y + 60, 150, 20).build());
		} else {
			addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 50, y + 60, 100, 20).build());
		}
	}

	private Component coresLabel() {
		return Component.translatable("zakoopt.bench.compare.cores", Component.translatable(ZakoOptConfig.values.corePin ? "zakoopt.bench.compare.cores.best" : "zakoopt.bench.compare.cores.all"));
	}

	private void moved(Button name) {
		Bench.teleport(Scene.views().get(view));
		name.setMessage(viewLabel());
	}

	private Component toggleLabel() {
		boolean on = ZakoOptConfig.values.enabled;
		return Component.translatable(on ? "zakoopt.bench.compare.toggle.on" : "zakoopt.bench.compare.toggle.off")
				.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
	}

	private Component viewLabel() {
		return Component.translatable("zakoopt.bench.view." + Scene.views().get(view).key());
	}

	@Override
	protected void closed() {
		done.complete(null);
	}
}
