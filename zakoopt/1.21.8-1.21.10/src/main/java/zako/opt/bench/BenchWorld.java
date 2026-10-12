package zako.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;

@UtilityClass
public class BenchWorld {
	public final boolean LEGACY_SNBT = false;

	public LevelSettings settings(String name) {
		return new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()),
				WorldDataConfiguration.DEFAULT);
	}

	public void leave(Minecraft mc) {
		mc.level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE);
		mc.disconnectWithSavingScreen();
	}
}
