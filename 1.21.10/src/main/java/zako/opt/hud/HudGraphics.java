package zako.opt.hud;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;

@UtilityClass
public class HudGraphics {
	public GuiGraphics create(Minecraft minecraft, GuiRenderState state) {
		return new GuiGraphics(minecraft, state);
	}
}
