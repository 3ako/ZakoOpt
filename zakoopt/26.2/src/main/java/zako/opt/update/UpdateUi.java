package zako.opt.update;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@UtilityClass
public class UpdateUi {
	public void open(String url) {
		Util.getPlatform().openUri(url);
	}

	public void setScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}
}
