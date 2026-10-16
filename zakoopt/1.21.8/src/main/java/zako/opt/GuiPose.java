package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphics;

@UtilityClass
public class GuiPose {
	public void push(GuiGraphics graphics, int x, int y, float scale) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y).scale(scale);
	}

	public void pop(GuiGraphics graphics) {
		graphics.pose().popMatrix();
	}
}
