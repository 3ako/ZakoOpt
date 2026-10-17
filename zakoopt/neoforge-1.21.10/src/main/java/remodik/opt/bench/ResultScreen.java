package remodik.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import remodik.opt.Canvas;
import remodik.opt.Panel;
import remodik.opt.ZakoOptions;
import remodik.opt.update.UpdateUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// every switch of the mod as it will be once applied; * marks what applying changes
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResultScreen extends Panel {
	static int PAD = 6, ROW = 14;

	Screen parent;
	Component gain;
	Map<String, Boolean> chosen;
	Map<String, Object> picks;
	List<String> changes;
	Runnable apply;
	List<ZakoOptions.Toggle> tested = new ArrayList<>(), other = new ArrayList<>();

	public ResultScreen(Screen parent, Component gain, Map<String, Boolean> chosen, Map<String, Object> picks, List<String> changes, Runnable apply) {
		super(Component.translatable("zakoopt.bench.done"));
		this.parent = parent;
		this.gain = gain;
		this.chosen = chosen;
		this.picks = picks;
		this.changes = changes;
		this.apply = apply;
		for (ZakoOptions.Group group : ZakoOptions.GROUPS) {
			for (ZakoOptions.Option option : group.available()) {
				if (option instanceof ZakoOptions.Toggle t && !t.key().equals("update_check")) {
					(chosen.containsKey(t.key()) ? tested : other).add(t);
				}
			}
		}
	}

	@Override
	protected void paint(Canvas c) {
		int w = Math.min(width - 2 * PAD, 520), x0 = (width - w) / 2, x1 = x0 + w, y0 = PAD, y1 = height - PAD;
		frame(c, x0, y0, x1, y1);
		c.text(title, x0 + 8, y0 + 7, STRONG);
		c.text(gain, x0 + 8, y0 + 19, TEXT);
		int top = y0 + 32;
		c.fill(x0, top, x1, top + 2, LINE);
		int bottom = y1 - 24;

		int y = beginScroll(c, x0, top + 2, x1, bottom) + 6;
		y = lines(c, Component.translatable("zakoopt.bench.legend"), x0 + 8, y, w - 16, MUTED) + 4;
		y = header(c, Component.translatable("zakoopt.bench.result.tested"), x0, x1, y);
		y = rows(c, tested, x0, x1, y);
		y = header(c, Component.translatable("zakoopt.bench.result.other"), x0, x1, y + 6);
		for (Map.Entry<String, Object> pick : picks.entrySet()) {
			if (pick.getValue() instanceof Integer distance) {
				c.text(Component.translatable("zakoopt.option." + pick.getKey()).append(": ").append(Component.translatable(unit(pick.getKey()), distance))
						.append(changes.contains(pick.getKey()) ? " *" : ""), x0 + 12, y + 2, TEXT);
				y += ROW;
			}
		}
		y = rows(c, other, x0, x1, y);
		endScroll(c, y + 6);

		c.fill(x0, bottom, x1, bottom + 2, LINE);
		int by = bottom + 6;
		if (changes.isEmpty()) {
			Component close = Component.translatable("gui.done");
			button(c, x1 - 6 - buttonWidth(close), by, close, this::onClose);
			return;
		}
		Component keep = Component.translatable("zakoopt.bench.keep");
		int kx = x1 - 6 - buttonWidth(keep);
		button(c, kx, by, keep, this::onClose);
		Component applyLabel = Component.translatable("zakoopt.bench.apply");
		int aw = buttonWidth(applyLabel);
		button(c, kx - 6 - aw, by, aw, applyLabel, ON, ACCENT, true, () -> {
			apply.run();
			onClose();
		});
	}

	private String unit(String key) {
		return ZakoOptions.GROUPS.stream().flatMap(g -> g.available().stream()).filter(o -> o.key().equals(key) && o instanceof ZakoOptions.Distance)
				.map(o -> ((ZakoOptions.Distance) o).unit()).findFirst().orElse("zakoopt.blocks");
	}

	private int header(Canvas c, Component text, int x0, int x1, int y) {
		c.text(text, x0 + 8, y + 2, STRONG);
		c.fill(x0 + 8, y + 12, x1 - 8, y + 13, LINE);
		return y + 16;
	}

	private int rows(Canvas c, List<ZakoOptions.Toggle> toggles, int x0, int x1, int y) {
		int column = (x1 - x0 - 16) / 2;
		for (int i = 0; i < toggles.size(); i++) {
			ZakoOptions.Toggle t = toggles.get(i);
			int x = x0 + 12 + (i % 2) * column;
			boolean on = chosen.containsKey(t.key()) ? chosen.get(t.key()) : picks.get(t.key()) instanceof Boolean b ? b : t.get().get();
			toggle(c, x, y + 2, on, false);
			Component name = Component.translatable("zakoopt.option." + t.key()).append(changes.contains(t.key()) ? " *" : "");
			c.text(firstLine(name, column - 30), x + 24, y + 2, on ? TEXT : MUTED);
			if (i % 2 == 1 || i == toggles.size() - 1) {
				y += ROW;
			}
		}
		return y;
	}

	@Override
	public void onClose() {
		UpdateUi.setScreen(parent);
	}
}
