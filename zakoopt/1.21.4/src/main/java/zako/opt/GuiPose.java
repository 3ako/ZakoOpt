package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphics;

@UtilityClass
public class GuiPose {
	public void push(GuiGraphics graphics, int x, int y, float scale) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1);
	}

	public void pop(GuiGraphics graphics) {
		graphics.pose().popPose();
	}
}
