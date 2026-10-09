package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j(topic = "zakoopt")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SodiumOptions implements ConfigEntryPoint {
	List<Identifier> toggles = new ArrayList<>();
	List<Supplier<Boolean>> bound = new ArrayList<>();
	@NonFinal
	ConfigBuilder builder;

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		this.builder = builder;
		log.info("Registering ZakoOpt options in Sodium. Канал автора: " + ZakoOptConfig.CHANNEL);
		var mod = builder.registerOwnModOptions()
				.setName("ZakoOpt " + ZakoOptions.VERSION)
				.formatVersion(version -> "@StarikZako")
				.setNonTintedIcon(Identifier.fromNamespaceAndPath("zakoopt", "icon.png"));
		boolean first = true;
		for (ZakoOptions.Page p : ZakoOptions.PAGES) {
			var page = builder.createOptionPage().setName(Component.translatable("zakoopt.page." + p.key()));
			if (first) {
				page.addOptionGroup(author()).addOptionGroup(group("all").addOption(master()));
				first = false;
			}
			boolean empty = true;
			for (ZakoOptions.Group g : p.groups()) {
				var available = g.available();
				if (available.isEmpty()) {
					continue;
				}
				var group = group(g.key());
				available.forEach(o -> group.addOption(option(o)));
				page.addOptionGroup(group);
				empty = false;
			}
			if (!empty || p == ZakoOptions.PAGES.getFirst()) {
				mod.addPage(page);
			}
		}
	}

	private net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder option(ZakoOptions.Option option) {
		return switch (option) {
			case ZakoOptions.Toggle t -> ZakoOptConfig.FOREIGN_HUD && t.key().equals("hud_cache")
					? bool(t.key(), OptionImpact.valueOf(t.impact().name()), t.set(), t.get()).setEnabled(false).setTooltip(Component.translatable("zakoopt.option.hud_cache.foreign"))
					: bool(t.key(), OptionImpact.valueOf(t.impact().name()), t.set(), t.get());
			case ZakoOptions.Distance d -> distance(d.key(), d.min(), d.max(), d.set(), d.get());
		};
	}

	private OptionGroupBuilder author() {
		return group("author").addOption(builder.createExternalButtonOption(Identifier.fromNamespaceAndPath("zakoopt", "channel"))
				.setName(Component.translatable("zakoopt.option.channel"))
				.setTooltip(Component.translatable("zakoopt.option.channel.tooltip"))
				.setScreenConsumer(screen -> ConfirmLinkScreen.confirmLinkNow(screen, ZakoOptConfig.CHANNEL)));
	}

	private OptionGroupBuilder group(String key) {
		return builder.createOptionGroup().setName(Component.translatable("zakoopt.group." + key));
	}

	private net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder bool(String key, OptionImpact impact, Consumer<Boolean> set, Supplier<Boolean> get) {
		Identifier id = Identifier.fromNamespaceAndPath("zakoopt", key);
		if (ZakoOptions.recommended(key)) {
			toggles.add(id);
			bound.add(get);
		}
		return builder.createBooleanOption(id)
				.setName(Component.translatable("zakoopt.option." + key))
				.setTooltip(Component.translatable("zakoopt.option." + key + ".tooltip"))
				.setImpact(impact)
				.setDefaultValue(get.get())
				.setStorageHandler(ZakoOptConfig::save)
				.setBinding(set, get);
	}

	// stages every toggle on apply; options apply in registration order, so the toggles after it pick the staged values up in the same pass
	@SuppressWarnings("unchecked")
	private net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder master() {
		return builder.createBooleanOption(Identifier.fromNamespaceAndPath("zakoopt", "all"))
				.setName(Component.translatable("zakoopt.option.all"))
				.setTooltip(Component.translatable("zakoopt.option.all.tooltip"))
				.setImpact(OptionImpact.HIGH)
				.setDefaultValue(true)
				.setStorageHandler(ZakoOptConfig::save)
				.setBinding(b -> toggles.forEach(id -> ((StatefulOption<Boolean>) ConfigManager.CONFIG.getOption(id)).modifyValue(b)), () -> bound.stream().allMatch(Supplier::get));
	}

	private net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder distance(String key, int min, int max, Consumer<Integer> set, Supplier<Integer> get) {
		return builder.createIntegerOption(Identifier.fromNamespaceAndPath("zakoopt", key))
				.setName(Component.translatable("zakoopt.option." + key))
				.setTooltip(Component.translatable("zakoopt.option." + key + ".tooltip"))
				.setRange(min, max, 1)
				.setValueFormatter(i -> Component.translatable("zakoopt.blocks", i))
				.setDefaultValue(get.get())
				.setStorageHandler(ZakoOptConfig::save)
				.setBinding(set, get);
	}
}
