package zako.opt.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;

import java.util.Arrays;

// many fills as one GUI element: the layer search runs once instead of once per fill against all the others
public record FillBatch(Matrix3x2f pose, int[] rects, ScreenRectangle scissorArea, ScreenRectangle bounds)
		implements GuiElementRenderState {
	public static void submit(GuiGraphics graphics, int[] rects, int count) {
		if (count == 0) {
			return;
		}
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
		for (int i = 0; i < count * 5; i += 5) {
			x0 = Math.min(x0, rects[i]);
			y0 = Math.min(y0, rects[i + 1]);
			x1 = Math.max(x1, rects[i + 2]);
			y1 = Math.max(y1, rects[i + 3]);
		}
		Matrix3x2f pose = new Matrix3x2f(graphics.pose());
		ScreenRectangle scissor = graphics.scissorStack.peek();
		ScreenRectangle area = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
		graphics.guiRenderState.submitGuiElement(new FillBatch(pose, Arrays.copyOf(rects, count * 5), scissor,
				scissor != null ? scissor.intersection(area) : area));
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		for (int i = 0; i < rects.length; i += 5) {
			int x0 = rects[i], y0 = rects[i + 1], x1 = rects[i + 2], y1 = rects[i + 3], argb = rects[i + 4];
			consumer.addVertexWith2DPose(pose, x0, y0).setColor(argb);
			consumer.addVertexWith2DPose(pose, x0, y1).setColor(argb);
			consumer.addVertexWith2DPose(pose, x1, y1).setColor(argb);
			consumer.addVertexWith2DPose(pose, x1, y0).setColor(argb);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI;
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}
}
