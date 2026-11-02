package zako.opt.particle;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

@UtilityClass
public class CameraPosition {
	public Vec3 get() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().position();
	}
}
