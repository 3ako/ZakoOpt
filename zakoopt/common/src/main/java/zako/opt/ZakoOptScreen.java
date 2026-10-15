package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import zako.opt.bench.Bench;
import zako.opt.feature.Feature;
import zako.opt.feature.Features;
import zako.opt.update.UpdateUi;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ZakoOptScreen extends Panel {
	static int BAR = 22, ROW = 16, PAD = 6;

	Screen parent;
	List<Section> sections = new ArrayList<>();
	@NonFinal
	int selected;

	private record Item(Feature feature, ZakoOptions.Toggle toggle, List<ZakoOptions.Option> settings) {
	}

	private record Section(ZakoOptions.Group group, List<Item> items) {
	}

	public ZakoOptScreen(Screen parent) {
		super(Component.literal("ZakoOpt"));
		this.parent = parent;
		for (ZakoOptions.Group group : ZakoOptions.GROUPS) {
			List<Item> items = new ArrayList<>();
			for (ZakoOptions.Option option : group.available()) {
				Feature feature = Features.byKey(option.key());
				if (feature != null) {
					items.add(new Item(feature, (ZakoOptions.Toggle) option, new ArrayList<>()));
				} else if (!items.isEmpty()) {
					items.getLast().settings().add(option);
				}
			}
			if (!items.isEmpty()) {
				sections.add(new Section(group, items));
			}
		}
	}

	@Override
	protected void changed() {
		ZakoOptConfig.save();
	}

	@Override
	protected void paint(Canvas c) {
		int w = Math.min(width - 2 * PAD, 520), x0 = (width - w) / 2, x1 = x0 + w, y0 = PAD, y1 = height - PAD;
		frame(c, x0, y0, x1, y1);

		c.text(Component.literal("ZakoOpt"), x0 + 8, y0 + 7, STRONG);
		int vx = x0 + 8 + font.width("ZakoOpt") + 5;
		c.text(Component.literal(ZakoOptions.VERSION), vx, y0 + 7, DIM);
		int lx = vx + font.width(ZakoOptions.VERSION) + 8;
		Component author = Component.literal("@StarikZako");
		c.text(author, lx, y0 + 7, over(lx, y0 + 5, lx + font.width(author), y0 + 17) ? STRONG : LINK);
		hit(lx, y0 + 5, lx + font.width(author), y0 + 17, () -> ConfirmLinkScreen.confirmLinkNow(this, ZakoOptConfig.CHANNEL), null);

		boolean inWorld = minecraft.level != null;
		boolean benchOk = !inWorld && !Bench.running();
		Component bench = Component.translatable("zakoopt.bench.button");
		int bx = x1 - 6 - buttonWidth(bench);
		boolean overBench = button(c, bx, y0 + 4, buttonWidth(bench), bench, BUTTON, BUTTON_FRAME, benchOk, () -> Bench.confirm(this));
		Component master = Component.translatable(ZakoOptConfig.enabled ? "zakoopt.screen.on" : "zakoopt.screen.off");
		int masterWidth = Math.max(buttonWidth(master), buttonWidth(Component.translatable("zakoopt.screen.on")));
		boolean overMaster = button(c, bx - 6 - masterWidth, y0 + 4, masterWidth, master, ZakoOptConfig.enabled ? ON : MASTER_OFF, ZakoOptConfig.enabled ? ACCENT : MASTER_OFF_FRAME, true,
				() -> ZakoOptConfig.enabled = !ZakoOptConfig.enabled);
		c.fill(x0, y0 + BAR, x1, y0 + BAR + 2, LINE);

		int side = 0;
		for (Section s : sections) {
			side = Math.max(side, font.width(s.group().type().title()) + font.width(s.items().size() + "/" + s.items().size()) + 22);
		}
		int top = y0 + BAR + 2, bottom = y1 - BAR - 2, sx1 = x0 + Math.min(side, w / 3);
		c.fill(sx1, top, sx1 + 2, bottom, LINE);
		for (int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			int ry = top + 4 + i * ROW;
			boolean over = over(x0, ry, sx1, ry + ROW);
			if (i == selected) {
				c.fill(x0, ry, sx1, ry + ROW, SELECTED);
				c.fill(x0, ry, x0 + 2, ry + ROW, ACCENT);
			} else if (over) {
				c.fill(x0, ry, sx1, ry + ROW, HOVER);
			}
			c.text(s.group().type().title(), x0 + 8, ry + 4, i == selected ? STRONG : over ? TEXT : MUTED);
			String count = s.items().stream().filter(it -> it.feature().value && it.feature().blocked() == null).count() + "/" + s.items().size();
			c.text(Component.literal(count), sx1 - 6 - font.width(count), ry + 4, MUTED);
			int index = i;
			hit(x0, ry, sx1, ry + ROW, () -> {
				selected = index;
				resetScroll();
			}, null);
		}

		int y = beginScroll(c, sx1 + 2, top, x1, bottom);
		for (Item item : sections.get(selected).items()) {
			y = item(c, item, sx1 + 2, x1, y, top, bottom);
		}
		endScroll(c, y);

		c.fill(x0, bottom, x1, bottom + 2, LINE);
		int by = bottom + 6;
		Component done = Component.translatable("gui.done");
		int dx = x1 - 6 - buttonWidth(done);
		button(c, dx, by, done, this::onClose);
		Component defaults = Component.translatable("zakoopt.screen.defaults");
		int defaultsX = dx - 6 - buttonWidth(defaults);
		button(c, defaultsX, by, defaults, this::confirmDefaults);
		Component hint = overMaster ? Component.translatable("zakoopt.option.enabled.tooltip")
				: !overBench ? null : Component.translatable(benchOk ? "zakoopt.bench.button.tooltip" : inWorld ? "zakoopt.bench.button.in_world" : "zakoopt.bench.running");
		if (hint != null) {
			List<FormattedCharSequence> lines = font.split(hint, defaultsX - x0 - 16);
			int hy = lines.size() > 1 ? bottom + 4 : bottom + 9;
			for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
				c.text(line, x0 + 8, hy, MUTED);
				hy += 9;
			}
		}
	}

	private int item(Canvas c, Item item, int x0, int x1, int y, int top, int bottom) {
		Feature f = item.feature();
		Component blocked = Blocked.reason(f.key());
		boolean on = f.value && blocked == null;
		Component text = blocked != null ? blocked : Component.translatable("zakoopt.option." + f.key() + ".tooltip");
		List<ZakoOptions.Option> settings = on ? item.settings() : List.of();
		int height = 4 + 9 + 3 + lineCount(text, x1 - x0 - 48) * 10 + settings.size() * 14 + 5;
		if (y + height <= top || y >= bottom) {
			return y + height;
		}
		if (over(x0, y, x1, y + height)) {
			c.fill(x0, y, x1, y + height, HOVER);
		}
		toggle(c, x0 + 10, y + 5, on, blocked != null);
		c.text(Component.translatable("zakoopt.option." + f.key()), x0 + 34, y + 4, blocked != null ? DIM : STRONG);
		int pips = f.data().impact().ordinal() + 1;
		for (int i = 0; i < 3; i++) {
			int px = x1 - 10 - (3 - i) * 5;
			c.fill(px, y + 4, px + 3, y + 12, blocked == null && i < pips ? PIP : OFF);
		}
		if (blocked == null) {
			hit(x0, y, x1, y + 14, () -> f.value = !f.value, null);
		}
		int ly = lines(c, text, x0 + 34, y + 16, x1 - x0 - 48, blocked != null ? BLOCKED : MUTED);
		for (ZakoOptions.Option s : settings) {
			setting(c, s, x0 + 34, x1 - 12, ly + 2);
			ly += 14;
		}
		c.fill(x0 + 6, y + height - 1, x1 - 6, y + height, LINE);
		return y + height;
	}

	private void confirmDefaults() {
		UpdateUi.setScreen(new ConfirmPanel(yes -> {
			if (yes) {
				Features.restore(Features.defaults());
				changed();
			}
			UpdateUi.setScreen(this);
		}, Component.translatable("zakoopt.screen.defaults.title"), Component.translatable("zakoopt.screen.defaults.confirm"),
				Component.translatable("zakoopt.screen.defaults.yes"), Component.translatable("gui.cancel")));
	}

	@Override
	public void onClose() {
		UpdateUi.setScreen(parent);
	}
}
