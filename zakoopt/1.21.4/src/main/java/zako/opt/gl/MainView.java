package zako.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class MainView {
	public RenderTarget target() {
		return Minecraft.getInstance().getMainRenderTarget();
	}

	public Vec3 camera() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
	}
}
