package zako.opt.bench;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import zako.opt.OptionsHeader;
import zako.opt.ZakoOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// every switch of the mod, green on and red off as they will be once applied; * marks what applying changes
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResultScreen extends OptionsSubScreen {
	Component gain;
	Map<String, Boolean> chosen;
	Map<String, Object> picks;
	List<String> changes;
	Runnable apply;

	public ResultScreen(Screen parent, Component gain, Map<String, Boolean> chosen, Map<String, Object> picks, List<String> changes, Runnable apply) {
		super(parent, Minecraft.getInstance().options, Component.translatable("zakoopt.bench.done"));
		this.gain = gain;
		this.chosen = chosen;
		this.picks = picks;
		this.changes = changes;
		this.apply = apply;
	}

	@Override
	protected void addTitle() {
		layout.setHeaderHeight(48);
		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.addChild(new StringWidget(title, font), s -> s.alignHorizontallyCenter());
		header.addChild(new StringWidget(gain, font), s -> s.alignHorizontallyCenter());
		header.addChild(new StringWidget(Component.translatable("zakoopt.bench.legend").withStyle(ChatFormatting.GRAY), font), s -> s.alignHorizontallyCenter());
	}

	@Override
	protected void addOptions() {
		List<ZakoOptions.Toggle> tested = new ArrayList<>(), other = new ArrayList<>();
		for (ZakoOptions.Page page : ZakoOptions.PAGES) {
			for (ZakoOptions.Group group : page.groups()) {
				for (ZakoOptions.Option option : group.available()) {
					if (option instanceof ZakoOptions.Toggle t && !t.key().equals("update_check")) {
						(chosen.containsKey(t.key()) ? tested : other).add(t);
					}
				}
			}
		}
		OptionsHeader.add(list, Component.translatable("zakoopt.bench.result.tested").withStyle(ChatFormatting.BOLD));
		rows(tested);
		OptionsHeader.add(list, Component.translatable("zakoopt.bench.result.other").withStyle(ChatFormatting.BOLD));
		picks.forEach((key, value) -> {
			if (value instanceof Integer distance) {
				list.addSmall(List.<AbstractWidget>of(new StringWidget(310, 12, Component.translatable("zakoopt.option." + key).append(": ")
						.append(Component.translatable("zakoopt.blocks", distance)).append(changes.contains(key) ? " *" : ""), font)));
			}
		});
		rows(other);
	}

	private void rows(List<ZakoOptions.Toggle> toggles) {
		for (int i = 0; i < toggles.size(); i += 2) {
			List<AbstractWidget> row = new ArrayList<>();
			for (ZakoOptions.Toggle t : toggles.subList(i, Math.min(i + 2, toggles.size()))) {
				boolean on = chosen.containsKey(t.key()) ? chosen.get(t.key()) : picks.get(t.key()) instanceof Boolean b ? b : t.get().get();
				Component name = Component.translatable("zakoopt.option." + t.key()).append(changes.contains(t.key()) ? " *" : "")
						.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
				row.add(new StringWidget(150, 12, name, font));
			}
			list.addSmall(row);
		}
	}

	@Override
	protected void addFooter() {
		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		if (changes.isEmpty()) {
			footer.addChild(Button.builder(Component.translatable("gui.done"), b -> onClose()).width(200).build());
			return;
		}
		footer.addChild(Button.builder(Component.translatable("zakoopt.bench.apply"), b -> {
			apply.run();
			onClose();
		}).width(150).build());
		footer.addChild(Button.builder(Component.translatable("zakoopt.bench.keep"), b -> onClose()).width(150).build());
	}
}
