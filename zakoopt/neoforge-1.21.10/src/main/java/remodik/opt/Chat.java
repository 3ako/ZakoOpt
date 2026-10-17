package remodik.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// a line in the player's chat, only on this side
@UtilityClass
public class Chat {
	public boolean ready() {
		return Minecraft.getInstance().player != null;
	}

	public void show(Component text) {
		Minecraft.getInstance().gui.getChat().addMessage(text);
	}
}
