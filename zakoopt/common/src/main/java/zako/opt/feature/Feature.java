package zako.opt.feature;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import zako.opt.MixinPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

// one switch of the mod: its @FeatureData says where it shows and how it is stored, its @Setting fields are its extra
// settings, and a subclass adds whatever logic it needs on top of the plain on/off
@Getter
@Accessors(fluent = true)
public abstract class Feature {
	private final FeatureData data = getClass().getAnnotation(FeatureData.class);
	private final String property = "zakoopt." + data.key();
	private final List<Field> settings = Arrays.stream(getClass().getFields()).filter(f -> f.isAnnotationPresent(Setting.class)).toList();
	// a class lookup per mixin: refresh() reads it every frame, so it is resolved once
	private final boolean available = data.mixins().length == 0 || Arrays.stream(data.mixins()).anyMatch(MixinPlugin::applies);
	// as set by the player and stored in the config
	public boolean value = data.enabled();
	// what the game runs with this frame; hot code reads only this, and a worker thread reads it too
	public volatile boolean on;

	// why it cannot work on this machine, as a translation key or a mod name; null: it can
	public String blocked() {
		return null;
	}

	// once a frame on the render thread
	public void refresh() {
		on = flag(value) && blocked() == null && available();
	}

	public String key() {
		return data.key();
	}

	// this feature's -Dzakoopt.<key> over its configured value; all of them off with the master switch
	protected boolean flag(boolean configured) {
		return flag0(property, configured);
	}

	// another -Dzakoopt.<name> switch a feature keeps for one of its parts
	protected boolean flag(String name, boolean configured) {
		return flag0("zakoopt." + name, configured);
	}

	private boolean flag0(String property, boolean configured) {
		if (!Features.active) {
			return false;
		}
		String p = System.getProperty(property);
		return p == null ? configured : Boolean.parseBoolean(p);
	}

	@SneakyThrows(IllegalAccessException.class)
	public Object get(Field setting) {
		return setting.get(this);
	}

	@SneakyThrows(IllegalAccessException.class)
	public void set(Field setting, Object value) {
		setting.set(this, value);
	}
}
