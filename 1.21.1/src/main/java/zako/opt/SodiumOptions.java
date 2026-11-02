package zako.opt;

import lombok.extern.slf4j.Slf4j;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import zako.opt.update.UpdateUi;

@Slf4j(topic = "zakoopt")
public class SodiumOptions implements ConfigEntryPoint {
	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		// a Sodium older or newer than the one this was built against may lack a call; failing here would abort the resource reload
		try {
			register(builder);
		} catch (LinkageError e) {
			log.warn("This Sodium version has no room for the ZakoOpt page; settings stay in Mod Menu", e);
		}
	}

	private void register(ConfigBuilder builder) {
		var open = builder.createExternalButtonOption(ResourceLocation.fromNamespaceAndPath("zakoopt", "open"))
				.setName(Component.translatable("zakoopt.screen.open"))
				.setTooltip(Component.translatable("zakoopt.screen.open.tooltip"))
				.setScreenConsumer(screen -> UpdateUi.setScreen(new ZakoOptScreen(screen)));
		var mod = builder.registerOwnModOptions();
		ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("zakoopt", "icon.png");
		try {
			mod.setNonTintedIcon(icon);
		} catch (NoSuchMethodError e) {
			// before Sodium 0.8.6
			mod.setIcon(icon);
		}
		mod.addPage(builder.createOptionPage().setName(Component.literal("ZakoOpt"))
						.addOptionGroup(builder.createOptionGroup().addOption(open)));
	}
}
