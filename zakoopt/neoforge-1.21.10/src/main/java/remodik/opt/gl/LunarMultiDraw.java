package remodik.opt.gl;

import remodik.opt.feature.Features;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

// Lunar caches "NVIDIA on Windows" in this field on first draw and then unrolls every multi-draw into single draws;
// answering false up front keeps the plain multi-draw
@UtilityClass
public class LunarMultiDraw {
	private final ClassValue<Field> FIELD = new ClassValue<>() {
		@Override
		protected Field computeValue(Class<?> type) {
			try {
				Field field = type.getDeclaredField("lunar$unroll");
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException | RuntimeException e) {
				return null;
			}
		}
	};

	public void keep(Object owner) {
		Field field = Features.LUNAR_MULTIDRAW.on ? FIELD.get(owner.getClass()) : null;
		if (field != null) {
			try {
				field.set(owner, Boolean.FALSE);
			} catch (IllegalAccessException ignored) {
				// Lunar changed the field: leave its behaviour alone
			}
		}
	}
}
