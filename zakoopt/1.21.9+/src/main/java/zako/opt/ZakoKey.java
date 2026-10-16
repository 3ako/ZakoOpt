package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

@UtilityClass
public class ZakoKey {
	public final KeyMapping MONITOR = new KeyMapping("key.zakoopt.monitor", 295,
			KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("zakoopt", "zakoopt")));
}
