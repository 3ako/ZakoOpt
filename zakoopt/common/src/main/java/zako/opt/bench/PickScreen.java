package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import zako.opt.ZakoOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// a look-versus-speed setting the player picks by eye over the scene, with the live frame rate above the controls
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PickScreen extends BenchScreen {
	List<ZakoOptions.Option> options;
	String hint;
	CompletableFuture<Void> done;
	@NonFinal
	boolean touched;
	@NonFinal
	MultiLineTextWidget text;
	@NonFinal
	StringWidget fps;
	@NonFinal
	int ticks;

	public PickScreen(List<ZakoOptions.Option> options, String hint, CompletableFuture<Void> done) {
		super(Component.translatable(hint));
		this.options = options;
		this.hint = hint;
		this.done = done;
	}

	@Override
	protected void init() {
		text = addRenderableWidget(new MultiLineTextWidget(Component.translatable(hint + ".hint"), font).setMaxWidth(Math.min(300, width - 40)).setCentered(true));
		text.setPosition(width / 2 - text.getWidth() / 2, height / 2 - text.getHeight() / 2 - 20);
		text.visible = !touched;
		int y = height - 56 - options.size() * 24;
		fps = addRenderableWidget(new StringWidget(Component.empty(), font));
		fps.setY(y - 16);
		for (ZakoOptions.Option option : options) {
			addRenderableWidget(widget(option).createButton(minecraft.options, width / 2 - 100, y, 200));
			y += 24;
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 50, y, 100, 20).build());
		updateFps();
	}

	private OptionInstance<?> widget(ZakoOptions.Option option) {
		String key = "zakoopt.option." + option.key();
		return switch (option) {
			case ZakoOptions.Toggle t -> OptionInstance.createBoolean(key, t.get().get(), b -> {
				t.set().accept(b);
				touch();
			});
			case ZakoOptions.Distance d -> new OptionInstance<>(key, OptionInstance.noTooltip(),
					(caption, i) -> Options.genericValueLabel(caption, Component.translatable("zakoopt.blocks", i)),
					new OptionInstance.IntRange(d.min(), d.max()), d.get().get(), i -> {
				d.set().accept(i);
				touch();
			});
		};
	}

	private void touch() {
		touched = true;
		text.visible = false;
	}

	@Override
	public void tick() {
		if (++ticks % 10 == 0) {
			updateFps();
		}
	}

	private void updateFps() {
		Component message = Component.literal(Math.round(Bench.recentFps()) + " FPS");
		fps.setMessage(message);
		fps.setWidth(font.width(message));
		fps.setX(width / 2 - fps.getWidth() / 2);
	}

	@Override
	protected List<int[]> rects() {
		List<int[]> rects = new ArrayList<>();
		if (text.visible) {
			rects.add(new int[]{text.getX() - 8, text.getY() - 6, text.getX() + text.getWidth() + 8, text.getY() + text.getHeight() + 6, 0xB0000000});
		}
		rects.add(new int[]{fps.getX() - 4, fps.getY() - 3, fps.getX() + fps.getWidth() + 4, fps.getY() + 11, 0xB0000000});
		return rects;
	}

	@Override
	public void onClose() {
		done.complete(null);
		super.onClose();
	}
}
