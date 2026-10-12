package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class DebugToggle {
	public void toggle() {
		Minecraft.getInstance().debugEntries.toggleF3Visible();
	}

	public boolean visible() {
		return Minecraft.getInstance().debugEntries.isF3Visible();
	}
}
