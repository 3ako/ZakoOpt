package zako.opt.block;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

@UtilityClass
public class Terrain {
	public void rebuild() {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.level != null && mc.levelRenderer != null) {
			mc.levelRenderer.allChanged();
		}
	}
}
