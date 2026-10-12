package zako.opt;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j(topic = "zakoopt")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SodiumOptions implements ConfigEntryPoint {
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
			case ZakoOptions.Toggle t -> {
				var built = bool(t.key(), OptionImpact.valueOf(t.impact().name()), t.set(), t.get());
				Component blocked = Blocked.reason(t.key());
				yield blocked == null ? built : built.setEnabled(false).setTooltip(Component.empty()
						.append(blocked.copy().withStyle(net.minecraft.ChatFormatting.RED)).append("\n\n").append(Component.translatable("zakoopt.option." + t.key() + ".tooltip")));
			}
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
		return builder.createBooleanOption(Identifier.fromNamespaceAndPath("zakoopt", key))
				.setName(Component.translatable("zakoopt.option." + key))
				.setTooltip(Component.translatable("zakoopt.option." + key + ".tooltip"))
				.setImpact(impact)
				.setDefaultValue(get.get())
				.setStorageHandler(ZakoOptConfig::save)
				.setBinding(set, get);
	}

	private net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder master() {
		return builder.createBooleanOption(Identifier.fromNamespaceAndPath("zakoopt", "enabled"))
				.setName(Component.translatable("zakoopt.option.enabled"))
				.setTooltip(Component.translatable("zakoopt.option.enabled.tooltip"))
				.setImpact(OptionImpact.HIGH)
				.setDefaultValue(true)
				.setStorageHandler(ZakoOptConfig::save)
				.setBinding(b -> ZakoOptConfig.values.enabled = b, () -> ZakoOptConfig.values.enabled);
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
