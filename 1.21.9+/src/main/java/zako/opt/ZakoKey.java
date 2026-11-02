package zako.opt;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@UtilityClass
public class ZakoKey {
	public final KeyMapping MONITOR = new KeyMapping("key.zakoopt.monitor", InputConstants.KEY_F6,
			KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("zakoopt", "zakoopt")));

	public boolean shift() {
		return Minecraft.getInstance().hasShiftDown();
	}
}
