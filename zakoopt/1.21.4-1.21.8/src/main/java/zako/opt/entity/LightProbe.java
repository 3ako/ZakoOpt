package zako.opt.entity;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;

@UtilityClass
public class LightProbe {
	private final String NAME = FabricLoader.getInstance().getMappingResolver()
			.mapMethodName("intermediary", "net.minecraft.class_1297", "method_31166", "(F)Lnet/minecraft/class_243;");

	// true: the class probes light at the eyes, as Entity does
	private final ClassValue<Boolean> PLAIN = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			try {
				return type.getMethod(NAME, float.class).getDeclaringClass() == Entity.class;
			} catch (NoSuchMethodException e) {
				return false;
			}
		}
	};

	public boolean plain(Entity entity) {
		return PLAIN.get(entity.getClass());
	}
}
