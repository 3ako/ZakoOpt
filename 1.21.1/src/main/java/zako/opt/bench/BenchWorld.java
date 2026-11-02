package zako.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;

@UtilityClass
public class BenchWorld {
	public final boolean LEGACY_SNBT = true;

	public LevelSettings settings(String name) {
		return new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(),
				WorldDataConfiguration.DEFAULT);
	}

	public void leave(Minecraft mc) {
		mc.level.disconnect();
		mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
	}
}
