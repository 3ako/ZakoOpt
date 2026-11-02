package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.network.chat.Component;
import zako.opt.Canvas;
import zako.opt.Panel;
import zako.opt.ZakoOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// a look-versus-speed setting the player picks by eye over the scene, with the live frame rate above the controls
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PickScreen extends Panel {
	List<ZakoOptions.Option> options;
	String hint;
	CompletableFuture<Void> done;
	@NonFinal
	boolean touched;

	public PickScreen(List<ZakoOptions.Option> options, String hint, CompletableFuture<Void> done) {
		super(Component.translatable(hint));
		this.options = options;
		this.hint = hint;
		this.done = done;
	}

	@Override
	protected boolean overScene() {
		return true;
	}

	@Override
	protected void changed() {
		touched = true;
	}

	@Override
	protected void paint(Canvas c) {
		if (!touched) {
			Component text = Component.translatable(hint + ".hint");
			int tw = Math.min(300, width - 40), th = lineCount(text, tw) * 10;
			int tx = width / 2 - tw / 2, ty = height / 2 - th / 2 - 20;
			frame(c, tx - 8, ty - 6, tx + tw + 8, ty + th + 4);
			lines(c, text, tx, ty, tw, TEXT);
		}
		int w = Math.min(300, width - 16), x0 = width / 2 - w / 2, x1 = x0 + w;
		int h = 8 + 12 + 12 + options.size() * 16 + 4 + BUTTON_HEIGHT + 8, y0 = height - 8 - h;
		frame(c, x0, y0, x1, y0 + h);
		c.text(firstLine(title, w - 16), x0 + 8, y0 + 8, STRONG);
		c.text(Component.literal(Math.round(Bench.recentFps()) + " FPS"), x0 + 8, y0 + 20, MUTED);
		int y = y0 + 34;
		for (ZakoOptions.Option option : options) {
			setting(c, option, x0 + 8, x1 - 8, y);
			y += 16;
		}
		Component close = Component.translatable("gui.done");
		button(c, x0 + w / 2 - 40, y + 2, 80, close, BUTTON, BUTTON_FRAME, true, this::onClose);
	}

	@Override
	public void onClose() {
		done.complete(null);
		super.onClose();
	}
}
