package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ZakoOptScreen extends OptionsSubScreen {
	@NonFinal
	String query = "";
	@NonFinal
	EditBox search;

	public ZakoOptScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.literal("ZakoOpt " + ZakoOptions.VERSION + " · @StarikZako"));
	}

	@Override
	protected void addTitle() {
		layout.setHeaderHeight(52);
		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(6));
		header.addChild(new StringWidget(title, font), s -> s.alignHorizontallyCenter());
		search = header.addChild(new EditBox(font, 0, 0, 200, 18, Component.translatable("zakoopt.search")), s -> s.alignHorizontallyCenter());
		search.setHint(Component.translatable("zakoopt.search"));
		search.setValue(query);
		search.setResponder(text -> {
			query = text;
			refresh();
		});
	}

	@Override
	protected void addContents() {
		list = layout.addToContents(new Rows(minecraft, width, this));
		addOptions();
	}

	// rebuildWidgets() would add a second list to the screen's final layout, so only the rows are rebuilt
	private void refresh() {
		((Rows) list).clear();
		list.setScrollAmount(0);
		addOptions();
		repositionElements();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(search);
	}

	@Override
	protected void addOptions() {
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			Button channel = Button.builder(Component.translatable("zakoopt.option.channel"), b -> ConfirmLinkScreen.confirmLinkNow(this, ZakoOptConfig.CHANNEL))
					.tooltip(Tooltip.create(Component.translatable("zakoopt.option.channel.tooltip")))
					.build();
			OptionInstance<Boolean> all = OptionInstance.createBoolean("zakoopt.option.all", OptionInstance.cachedConstantTooltip(Component.translatable("zakoopt.option.all.tooltip")),
					ZakoOptions.recommended().stream().allMatch(t -> t.get().get()), b -> {
						ZakoOptions.recommended().forEach(t -> t.set().accept(b));
						ZakoOptConfig.save();
						refresh();
					});
			list.addSmall(all.createButton(options), channel);
		}
		for (ZakoOptions.Page page : ZakoOptions.PAGES) {
			for (ZakoOptions.Group group : page.groups()) {
				var shown = group.available().stream().filter(o -> q.isEmpty() || matches(o, q)).toList();
				if (shown.isEmpty()) {
					continue;
				}
				OptionsHeader.add(list, Component.translatable("zakoopt.group." + group.key()));
				OptionInstance<?>[] widgets = shown.stream().map(ZakoOptScreen::widget).toArray(OptionInstance[]::new);
				list.addSmall(widgets);
				for (int i = 0; i < widgets.length; i++) {
					if (ZakoOptConfig.FOREIGN_HUD && shown.get(i).key().equals("hud_cache")) {
						AbstractWidget button = list.findOption(widgets[i]);
						button.active = false;
					}
				}
			}
		}
	}

	private static class Rows extends OptionsList {
		Rows(Minecraft minecraft, int width, OptionsSubScreen screen) {
			super(minecraft, width, screen);
		}

		void clear() {
			clearEntries();
		}
	}

	private static boolean matches(ZakoOptions.Option option, String q) {
		String key = "zakoopt.option." + option.key();
		return (Component.translatable(key).getString() + " " + Component.translatable(key + ".tooltip").getString()).toLowerCase(Locale.ROOT).contains(q);
	}

	private static OptionInstance<?> widget(ZakoOptions.Option option) {
		String key = "zakoopt.option." + option.key();
		boolean foreignHud = ZakoOptConfig.FOREIGN_HUD && option.key().equals("hud_cache");
		Component text = Component.translatable(foreignHud ? "zakoopt.option.hud_cache.foreign" : key + ".tooltip");
		return switch (option) {
			case ZakoOptions.Toggle t -> OptionInstance.createBoolean(key,
					OptionInstance.cachedConstantTooltip(Component.empty().append(text).append("\n\n")
							.append(Component.translatable("zakoopt.impact", Component.translatable("zakoopt.impact." + t.impact().name().toLowerCase(Locale.ROOT))))),
					t.get().get(), b -> {
						t.set().accept(b);
						ZakoOptConfig.save();
					});
			case ZakoOptions.Distance d -> new OptionInstance<>(key, OptionInstance.cachedConstantTooltip(text),
					(caption, i) -> Options.genericValueLabel(caption, Component.translatable("zakoopt.blocks", i)),
					new OptionInstance.IntRange(d.min(), d.max()), d.get().get(), i -> {
				d.set().accept(i);
				ZakoOptConfig.save();
			});
		};
	}
}
