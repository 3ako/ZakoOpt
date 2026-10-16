package zako.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import zako.opt.mixin.KeyMappingCategoryAccessor;

import java.util.Collections;
import java.util.Map;

@UtilityClass
public class ZakoKey {
	private final String CATEGORY = "key.categories.zakoopt";
	public final KeyMapping MONITOR = create();

	// the controls screen sorts by category and fails on one missing from the order
	private KeyMapping create() {
		Map<String, Integer> order = KeyMappingCategoryAccessor.zakoopt$order();
		order.putIfAbsent(CATEGORY, Collections.max(order.values()) + 1);
		return new KeyMapping("key.zakoopt.monitor", 295, CATEGORY);
	}
}
