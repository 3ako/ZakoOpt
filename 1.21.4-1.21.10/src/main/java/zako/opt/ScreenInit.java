package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@UtilityClass
public class ScreenInit {
	public void init(Screen screen, int width, int height) {
		screen.init(Minecraft.getInstance(), width, height);
	}
}
