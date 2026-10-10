package zako.opt.hud;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// chat lines alternate a background fill and a text draw, and every switch of render type flushes the buffer; the text
// sits 50 above the fills, so drawing all fills first and all texts after looks the same with two flushes in total
@UtilityClass
public class ChatBatch {
	private record Text(Matrix4f pose, Font font, FormattedCharSequence text, int x, int y, int color) {
	}

	private final List<Text> DEFERRED = new ArrayList<>();

	public void defer(Matrix4f pose, Font font, FormattedCharSequence text, int x, int y, int color) {
		DEFERRED.add(new Text(new Matrix4f(pose), font, text, x, y, color));
	}

	public void clear() {
		DEFERRED.clear();
	}

	public void flush(GuiGraphics graphics) {
		for (Text t : DEFERRED) {
			graphics.pose().pushPose();
			graphics.pose().last().pose().set(t.pose);
			graphics.drawString(t.font, t.text, t.x, t.y, t.color);
			graphics.pose().popPose();
		}
		DEFERRED.clear();
	}
}
