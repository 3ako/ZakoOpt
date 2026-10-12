package zako.opt;

import lombok.extern.slf4j.Slf4j;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import zako.opt.update.UpdateUi;

@Slf4j(topic = "zakoopt")
public class SodiumOptions implements ConfigEntryPoint {
	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		log.info("Registering ZakoOpt options in Sodium. Канал автора: " + ZakoOptConfig.CHANNEL);
		// a Sodium older or newer than the one this was built against may lack a call; failing here would abort the resource reload
		try {
			register(builder);
		} catch (LinkageError e) {
			log.warn("This Sodium version has no room for the ZakoOpt page; settings stay in Mod Menu", e);
		}
	}

	private void register(ConfigBuilder builder) {
		var open = builder.createExternalButtonOption(Identifier.fromNamespaceAndPath("zakoopt", "open"))
				.setName(Component.translatable("zakoopt.screen.open"))
				.setTooltip(Component.translatable("zakoopt.screen.open.tooltip"))
				.setScreenConsumer(screen -> UpdateUi.setScreen(new ZakoOptScreen(screen)));
		var channel = builder.createExternalButtonOption(Identifier.fromNamespaceAndPath("zakoopt", "channel"))
				.setName(Component.translatable("zakoopt.option.channel"))
				.setTooltip(Component.translatable("zakoopt.option.channel.tooltip"))
				.setScreenConsumer(screen -> ConfirmLinkScreen.confirmLinkNow(screen, ZakoOptConfig.CHANNEL));
		var mod = builder.registerOwnModOptions()
				.setName("ZakoOpt " + ZakoOptions.VERSION)
				.formatVersion(version -> "@StarikZako");
		Identifier icon = Identifier.fromNamespaceAndPath("zakoopt", "icon.png");
		try {
			mod.setNonTintedIcon(icon);
		} catch (NoSuchMethodError e) {
			// before Sodium 0.8.6
			mod.setIcon(icon);
		}
		mod.addPage(builder.createOptionPage().setName(Component.literal("ZakoOpt"))
						.addOptionGroup(builder.createOptionGroup().addOption(open).addOption(channel)));
	}
}
