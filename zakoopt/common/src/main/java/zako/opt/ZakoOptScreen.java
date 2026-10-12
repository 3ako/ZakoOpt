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
import net.minecraft.client.gui.layouts.LayoutSettings;
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
		header.addChild(new StringWidget(title, font), LayoutSettings::alignHorizontallyCenter);
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
			OptionInstance<Boolean> all = OptionInstance.createBoolean("zakoopt.option.enabled", OptionInstance.cachedConstantTooltip(Component.translatable("zakoopt.option.enabled.tooltip")),
					ZakoOptConfig.values.enabled, b -> {
						ZakoOptConfig.values.enabled = b;
						ZakoOptConfig.save();
					});
			list.addSmall(all.createButton(options), channel);
			boolean inWorld = minecraft.level != null;
			Button bench = Button.builder(Component.translatable("zakoopt.bench.button"), b -> zako.opt.bench.Bench.confirm(this))
					.tooltip(Tooltip.create(Component.translatable(inWorld ? "zakoopt.bench.button.in_world" : "zakoopt.bench.button.tooltip")))
					.build();
			bench.active = !inWorld && !zako.opt.bench.Bench.running();
			list.addSmall(java.util.List.of(bench));
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
					if (Blocked.reason(shown.get(i).key()) != null) {
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
		Component blocked = Blocked.reason(option.key());
		Component text = blocked == null ? Component.translatable(key + ".tooltip")
				: Component.empty().append(blocked.copy().withStyle(net.minecraft.ChatFormatting.RED)).append("\n\n").append(Component.translatable(key + ".tooltip"));
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
