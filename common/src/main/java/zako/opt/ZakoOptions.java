package zako.opt;

import lombok.experimental.UtilityClass;
import zako.opt.feature.Feature;
import zako.opt.feature.FeatureGroup;
import zako.opt.feature.Features;
import zako.opt.feature.Range;
import zako.opt.feature.Setting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// the options screens' view of the features: groups of switches and sliders, built from the feature registry
@UtilityClass
public class ZakoOptions {
	public final String VERSION = Platform.loader().version("zakoopt");

	public sealed interface Option permits Toggle, Distance {
		String key();

		String[] requires();

		// an option is shown only if one of the mixins behind it is built for this Minecraft version and applied
		default boolean available() {
			return requires().length == 0 || Arrays.stream(requires())
					.anyMatch(MixinPlugin::applies);
		}
	}

	public record Toggle(String key, Consumer<Boolean> set, Supplier<Boolean> get, String... requires) implements Option {
	}

	public record Distance(String key, int min, int max, String unit, Consumer<Integer> set, Supplier<Integer> get, String... requires) implements Option {
	}

	public record Group(FeatureGroup type, List<Option> options) {
		public String key() {
			return type.key();
		}

		public List<Option> available() {
			return options.stream().filter(Option::available).toList();
		}
	}

	public final List<Group> GROUPS = Arrays.stream(FeatureGroup.values()).map(ZakoOptions::group).toList();

	// a feature's switch, then its own settings right under it
	private Group group(FeatureGroup group) {
		List<Option> options = new ArrayList<>();
		for (Feature f : Features.all()) {
			if (f.data().group() != group) {
				continue;
			}
			String[] requires = f.data().mixins();
			options.add(new Toggle(f.key(), b -> f.value = b, () -> f.value, requires));
			for (Field s : f.settings()) {
				options.add(setting(f, s, requires));
			}
		}
		return new Group(group, options);
	}

	// a new setting type gets its screen element here and its JSON form in Features.read / write
	private Option setting(Feature f, Field s, String[] requires) {
		String key = s.getAnnotation(Setting.class).key();
		if (s.getType() == boolean.class) {
			return new Toggle(key, b -> f.set(s, b), () -> (Boolean) f.get(s), requires);
		}
		Range range = s.getAnnotation(Range.class);
		if (s.getType() == int.class && range != null) {
			return new Distance(key, range.min(), range.max(), range.unit(), i -> f.set(s, i), () -> (Integer) f.get(s), requires);
		}
		throw new IllegalStateException("Setting " + key + ": a " + s.getType().getSimpleName() + " field" + (s.getType() == int.class ? " needs @Range" : " is not supported"));
	}
}
