package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class DebugToggle {
	public void toggle() {
		Minecraft.getInstance().getDebugOverlay().toggleOverlay();
	}

	public boolean visible() {
		return Minecraft.getInstance().getDebugOverlay().showDebugScreen();
	}
}
