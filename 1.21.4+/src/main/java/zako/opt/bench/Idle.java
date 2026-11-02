package zako.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class Idle {
	// the player keeps off the input for the whole test, and after a minute of that the game caps itself at 30 FPS
	public void keepAwake() {
		Minecraft.getInstance().getFramerateLimitTracker().onInputReceived();
	}
}
