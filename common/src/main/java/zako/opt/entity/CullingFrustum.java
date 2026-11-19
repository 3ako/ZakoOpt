package zako.opt.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.culling.Frustum;
import zako.opt.Platform;

import java.lang.reflect.Field;

// EntityCulling keeps the current frustum in a field of its own and sets it at the head of the vanilla entity loop. The
// parallel path cancels that loop, so the field is set from here instead: its culling worker would otherwise test every
// entity against the frustum of the last frame it saw. Without the field the feature stays off rather than risk that
@UtilityClass
public class CullingFrustum {
	private Field instanceField;
	private Field frustumField;
	private boolean resolved;
	private boolean failed;

	// false = EntityCulling is installed and its field could not be found; the caller must keep the vanilla loop
	public boolean ready() {
		if (!Platform.isModLoaded("entityculling")) {
			return true;
		}
		if (failed) {
			return false;
		}
		if (!resolved) {
			resolved = true;
			try {
				Class<?> base = Class.forName("dev.tr7zw.entityculling.EntityCullingModBase");
				Field instance = base.getDeclaredField("instance");
				instance.setAccessible(true);
				Field frustum = base.getDeclaredField("frustum");
				frustum.setAccessible(true);
				instanceField = instance;
				frustumField = frustum;
			} catch (ReflectiveOperationException | RuntimeException e) {
				failed = true;
				return false;
			}
		}
		return true;
	}

	public void update(Frustum frustum) {
		if (!ready()) {
			return;
		}
		if (frustumField == null) {
			return;
		}
		try {
			Object mod = instanceField.get(null);
			if (mod != null) {
				frustumField.set(mod, frustum);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			failed = true;
		}
	}
}
