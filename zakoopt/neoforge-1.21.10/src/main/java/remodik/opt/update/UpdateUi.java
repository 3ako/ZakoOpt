package remodik.opt.update;

import java.net.URI;
import lombok.experimental.UtilityClass;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@UtilityClass
public class UpdateUi {
	public void open(URI url) {
		Util.getPlatform().openUri(url);
	}

	public void setScreen(Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}
}
