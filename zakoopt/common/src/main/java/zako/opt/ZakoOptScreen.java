package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import zako.opt.bench.Bench;
import zako.opt.bench.MonitorScreen;
import zako.opt.feature.Feature;
import zako.opt.feature.Features;
import zako.opt.update.UpdateUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ZakoOptScreen extends Panel {
	static int BAR = 22, ROW = 16, PAD = 6, FIELD = 0xFFF5EEDF, FIELD_FOCUSED = 0xFFFDFAF3;

	Screen parent;
	List<Section> sections = new ArrayList<>();
	@NonFinal
	int selected;
	// the tooltip of the setting under the mouse; shown in the hint line
	@NonFinal
	Component settingHint;
	// what the "changes visuals" eye under the mouse says; shown in the hint line
	@NonFinal
	Component visualHint;
	@NonFinal
	String query = "";
	@NonFinal
	boolean focused;
	@NonFinal
	int fieldX0, fieldX1, fieldY;
	// where each group's header sits in the search results, for the group list to scroll to
	int[] heads;

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
		heads = new int[sections.size()];
	}

	@Override
	protected void changed() {
		ZakoOptConfig.save();
	}

	@Override
	protected void paint(Canvas c) {
		settingHint = null;
		int w = Math.min(width - 2 * PAD, 520), x0 = (width - w) / 2, x1 = x0 + w, y0 = PAD, y1 = height - PAD;
		frame(c, x0, y0, x1, y1);
		visualHint = null;

		c.text(Component.literal("ZakoOpt"), x0 + 8, y0 + 7, STRONG);
		int vx = x0 + 8 + font.width("ZakoOpt") + 5;
		c.text(Component.literal(ZakoOptions.VERSION), vx, y0 + 7, DIM);

		boolean inWorld = minecraft.level != null;
		boolean benchOk = !inWorld && !Bench.running();
		Component bench = Component.translatable("zakoopt.bench.button");
		int bx = x1 - 6 - buttonWidth(bench);
		boolean overBench = button(c, bx, y0 + 4, buttonWidth(bench), bench, BUTTON, BUTTON_FRAME, benchOk, () -> Bench.confirm(this));
		Component compare = Component.translatable(inWorld ? "zakoopt.monitor.button" : "zakoopt.bench.compare.button");
		bx -= 6 + buttonWidth(compare);
		boolean overCompare = button(c, bx, y0 + 4, buttonWidth(compare), compare, BUTTON, BUTTON_FRAME, !Bench.running(),
				() -> {
					if (inWorld) {
						UpdateUi.setScreen(new MonitorScreen());
					} else {
						Bench.compare(this);
					}
				});
		Component master = Component.translatable(ZakoOptConfig.enabled ? "zakoopt.screen.on" : "zakoopt.screen.off");
		int masterWidth = Math.max(buttonWidth(master), buttonWidth(Component.translatable("zakoopt.screen.on")));
		int masterX = bx - 6 - masterWidth;
		boolean overMaster = button(c, masterX, y0 + 4, masterWidth, master, ZakoOptConfig.enabled ? ON : MASTER_OFF, ZakoOptConfig.enabled ? ACCENT : MASTER_OFF_FRAME, true,
				() -> ZakoOptConfig.enabled = !ZakoOptConfig.enabled);
		searchField(c, vx + font.width(ZakoOptions.VERSION) + 8, masterX - 6, y0 + 4);
		c.fill(x0, y0 + BAR, x1, y0 + BAR + 2, LINE);

		String needle = norm(query.strip());
		boolean searching = !needle.isEmpty();
		List<List<Item>> found = new ArrayList<>();
		int total = 0;
		for (Section s : sections) {
			List<Item> items = searching ? s.items().stream().filter(it -> matches(it, needle)).toList() : s.items();
			found.add(items);
			total += items.size();
		}

		int side = 0;
		for (Section s : sections) {
			side = Math.max(side, font.width(s.group().type().title()) + font.width(s.items().size() + "/" + s.items().size()) + 22);
		}
		int top = y0 + BAR + 2, bottom = y1 - BAR - 2, sx1 = x0 + Math.min(side, w / 3);
		c.fill(sx1, top, sx1 + 2, bottom, LINE);
		for (int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			int ry = top + 4 + i * ROW;
			boolean empty = searching && found.get(i).isEmpty();
			boolean over = !empty && over(x0, ry, sx1, ry + ROW);
			boolean current = !searching && i == selected;
			if (current) {
				c.fill(x0, ry, sx1, ry + ROW, SELECTED);
				c.fill(x0, ry, x0 + 2, ry + ROW, ACCENT);
			} else if (over) {
				c.fill(x0, ry, sx1, ry + ROW, HOVER);
			}
			c.text(s.group().type().title(), x0 + 8, ry + 4, empty ? OFF : current ? STRONG : over ? TEXT : MUTED);
			String count = searching ? String.valueOf(found.get(i).size())
					: s.items().stream().filter(it -> it.feature().value && it.feature().blocked() == null).count() + "/" + s.items().size();
			c.text(Component.literal(count), sx1 - 6 - font.width(count), ry + 4, empty ? OFF : searching ? VISUAL : MUTED);
			int index = i;
			if (searching) {
				if (!empty) {
					hit(x0, ry, sx1, ry + ROW, () -> scrollTo(heads[index]), null);
				}
			} else {
				hit(x0, ry, sx1, ry + ROW, () -> {
					selected = index;
					resetScroll();
				}, null);
			}
		}

		int start = beginScroll(c, sx1 + 2, top, x1, bottom), y = start;
		if (!searching) {
			for (Item item : sections.get(selected).items()) {
				y = item(c, item, sx1 + 2, x1, y, top, bottom, needle);
			}
		} else {
			for (int i = 0; i < sections.size(); i++) {
				List<Item> items = found.get(i);
				if (items.isEmpty()) {
					continue;
				}
				heads[i] = y - start;
				c.text(Component.empty().append(sections.get(i).group().type().title()).append(" · " + items.size()), sx1 + 10, y + 5, MUTED);
				c.fill(sx1 + 2, y + 15, x1, y + 17, LINE);
				y += 17;
				for (Item item : items) {
					y = item(c, item, sx1 + 2, x1, y, top, bottom, needle);
				}
			}
		}
		endScroll(c, y);
		if (searching && total == 0) {
			int cx = (sx1 + 2 + x1) / 2, cy = (top + bottom) / 2 - 10;
			Component none = Component.translatable("zakoopt.search.none", query.strip());
			c.text(none, cx - font.width(none) / 2, cy, STRONG);
			Component noneHint = Component.translatable("zakoopt.search.none.hint");
			c.text(noneHint, cx - font.width(noneHint) / 2, cy + 12, MUTED);
		}

		c.fill(x0, bottom, x1, bottom + 2, LINE);
		int by = bottom + 6;
		Component done = Component.translatable("gui.done");
		int dx = x1 - 6 - buttonWidth(done);
		button(c, dx, by, done, this::onClose);
		Component defaults = Component.translatable("zakoopt.screen.defaults");
		int defaultsX = dx - 6 - buttonWidth(defaults);
		button(c, defaultsX, by, defaults, this::confirmDefaults);
		Component hint = settingHint != null ? settingHint : visualHint != null ? visualHint : overMaster ? Component.translatable("zakoopt.option.enabled.tooltip")
				: overCompare ? Component.translatable(Bench.running() ? "zakoopt.bench.running" : inWorld ? "zakoopt.monitor.tooltip" : "zakoopt.bench.compare.button.tooltip", ZakoKey.MONITOR.getTranslatedKeyMessage())
				: overBench ? Component.translatable(benchOk ? "zakoopt.bench.button.tooltip" : inWorld ? "zakoopt.bench.button.in_world" : "zakoopt.bench.running")
				: searching ? Component.translatable("zakoopt.search.esc") : null;
		if (hint != null) {
			List<FormattedCharSequence> lines = font.split(hint, defaultsX - x0 - 16);
			int hy = lines.size() > 1 ? bottom + 4 : bottom + 9;
			for (FormattedCharSequence line : lines.subList(0, Math.min(2, lines.size()))) {
				c.text(line, x0 + 8, hy, MUTED);
				hy += 9;
			}
		}
	}

	private int item(Canvas c, Item item, int x0, int x1, int y, int top, int bottom, String needle) {
		Feature f = item.feature();
		Component blocked = Blocked.reason(f.key());
		boolean on = f.value && blocked == null;
		Component text = blocked != null ? blocked : Component.translatable("zakoopt.option." + f.key() + ".tooltip");
		List<ZakoOptions.Option> settings = on ? item.settings() : List.of();
		Component title = Component.translatable("zakoopt.option." + f.key());
		int height = 4 + 9 + 3 + lineCount(text, x1 - x0 - 48) * 10 + settings.size() * 14 + 5;
		if (y + height <= top || y >= bottom) {
			return y + height;
		}
		if (over(x0, y, x1, y + height)) {
			c.fill(x0, y, x1, y + height, HOVER);
		}
		toggle(c, x0 + 10, y + 5, on, blocked != null);
		if (!needle.isEmpty()) {
			mark(c, text("zakoopt.option." + f.key()), needle, x0 + 34, y + 3);
		}
		c.text(title, x0 + 34, y + 4, blocked != null ? DIM : STRONG);
		if (f.data().visual()) {
			int ex = x1 - 10 - EYE[0].length(), ey = y + 5;
			pixels(c, EYE, ex, ey, blocked != null ? DIM : VISUAL);
			if (over(ex - 2, ey - 2, ex + EYE[0].length() + 2, ey + EYE.length + 2)) {
				visualHint = Component.translatable("zakoopt.visual." + f.key());
			}
		}
		int ly = lines(c, text, x0 + 34, y + 16, x1 - x0 - 48, blocked != null ? BLOCKED : MUTED);
		// the title and the description toggle; the settings rows below stay out so a near miss on a slider does not switch the feature off
		if (blocked == null) {
			hit(x0, y, x1, ly, () -> f.value = !f.value, null);
		}
		for (ZakoOptions.Option s : settings) {
			String tooltip = "zakoopt.option." + s.key() + ".tooltip";
			if (setting(c, s, x0 + 34, x1 - 12, ly + 2) && Language.getInstance().has(tooltip)) {
				settingHint = Component.translatable(tooltip);
			}
			ly += 14;
		}
		c.fill(x0 + 6, y + height - 1, x1 - 6, y + height, LINE);
		return y + height;
	}

	private void searchField(Canvas c, int left, int right, int y) {
		int w = Math.min(120, right - left);
		fieldX0 = right - w;
		fieldX1 = w < 40 ? fieldX0 : right;
		fieldY = y;
		if (w < 40) {
			return;
		}
		int x = fieldX0, tx = x + 6 + LENS[0].length();
		c.fill(x, y, right, y + BUTTON_HEIGHT, focused ? STRONG : over(x, y, right, y + BUTTON_HEIGHT) ? BUTTON_HOVER : BUTTON_FRAME);
		c.fill(x + 1, y + 1, right - 1, y + BUTTON_HEIGHT - 1, focused ? FIELD_FOCUSED : FIELD);
		pixels(c, LENS, x + 4, y + 3, MUTED);
		String shown = query;
		while (font.width(shown) > right - tx - 4) {
			shown = shown.substring(1);
		}
		int cx = tx + font.width(shown);
		if (query.isEmpty()) {
			Component placeholder = Component.translatable("zakoopt.search.focused");
			if (!focused || font.width(placeholder) > right - tx - 6) {
				placeholder = Component.translatable("zakoopt.search");
			}
			c.text(placeholder, tx + (focused ? 2 : 0), y + 3, DIM);
		} else {
			c.text(Component.literal(shown), tx, y + 3, STRONG);
		}
		if (focused && System.currentTimeMillis() / 500 % 2 == 0) {
			c.fill(cx, y + 2, cx + 1, y + 12, STRONG);
		}
	}

	private static String text(String key) {
		return Language.getInstance().getOrDefault(key);
	}

	private static String norm(String s) {
		return s.toLowerCase(Locale.ROOT).replace('ё', 'е');
	}

	private static boolean matches(Item item, String needle) {
		String key = item.feature().key();
		if (norm(text("zakoopt.option." + key)).contains(needle) || norm(text("zakoopt.option." + key + ".tooltip")).contains(needle)
				|| item.feature().data().visual() && norm(text("zakoopt.visual." + key)).contains(needle)) {
			return true;
		}
		for (ZakoOptions.Option s : item.settings()) {
			if (norm(text("zakoopt.option." + s.key())).contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private void mark(Canvas c, String text, String needle, int x, int y) {
		String n = norm(text);
		// lower-casing can change the length of some letters, and then the indexes would not map back
		if (n.length() != text.length()) {
			return;
		}
		for (int i = n.indexOf(needle); i >= 0; i = n.indexOf(needle, i + needle.length())) {
			int hx = x + font.width(text.substring(0, i));
			c.fill(hx - 1, y, hx + font.width(text.substring(i, i + needle.length())), y + 10, MARK);
		}
	}

	@Override
	protected boolean click(double x, double y, int button) {
		focused = button == 0 && x >= fieldX0 && x < fieldX1 && y >= fieldY && y < fieldY + BUTTON_HEIGHT;
		return super.click(x, y, button) || focused;
	}

	@Override
	protected boolean escape() {
		if (query.isEmpty()) {
			if (!focused) {
				return false;
			}
			focused = false;
			return true;
		}
		search("");
		focused = false;
		return true;
	}

	@Override
	protected boolean erase(boolean all) {
		if (query.isEmpty()) {
			return false;
		}
		search(all ? "" : query.substring(0, query.length() - 1));
		return true;
	}

	@Override
	protected boolean typed(int codepoint) {
		if (Character.isISOControl(codepoint) || codepoint == ' ' && query.isEmpty()) {
			return false;
		}
		if (query.length() < 64) {
			search(query + Character.toString(codepoint));
		}
		return true;
	}

	private void search(String query) {
		this.query = query;
		focused = true;
		resetScroll();
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

	// marks a feature that changes the picture
	private static final String[] EYE = {
			"....###....",
			"..##...##..",
			".#..###..#.",
			"#..#####..#",
			".#..###..#.",
			"..##...##..",
			"....###....",
	};

	private static final String[] LENS = {
			"..####...",
			".#....#..",
			"#......#.",
			"#......#.",
			"#......#.",
			"#......#.",
			".#....#..",
			"..####.#.",
			"........#",
	};

	private static void pixels(Canvas c, String[] image, int x, int y, int color) {
		for (int row = 0; row < image.length; row++) {
			String line = image[row];
			for (int i = 0; i < line.length(); i++) {
				if (line.charAt(i) != '#') {
					continue;
				}
				int start = i;
				while (i < line.length() && line.charAt(i) == '#') {
					i++;
				}
				c.fill(x + start, y + row, x + i, y + row + 1, color);
			}
		}
	}

	@Override
	public void onClose() {
		UpdateUi.setScreen(parent);
	}
}
