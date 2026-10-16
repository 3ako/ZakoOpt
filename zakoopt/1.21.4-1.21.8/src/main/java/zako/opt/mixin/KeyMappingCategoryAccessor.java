package zako.opt.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingCategoryAccessor {
	@Accessor("CATEGORY_SORT_ORDER")
	static Map<String, Integer> zakoopt$order() {
		throw new AssertionError();
	}
}
