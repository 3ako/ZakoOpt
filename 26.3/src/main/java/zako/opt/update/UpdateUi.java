package zako.opt.update;

import java.net.URI;
import lombok.experimental.UtilityClass;
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@UtilityClass
public class UpdateUi {
	public void open(URI url) {
		Blaze3D.openUri(url);
	}

	public void setScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}
}
