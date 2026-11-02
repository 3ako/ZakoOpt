package zako.opt;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// which installed mod some code belongs to, for naming it to the player; slow, meant for a one-off lookup when something is detected
@UtilityClass
public class Mods {
	private final Set<String> OURS = Set.of("minecraft", "java", "fabricloader", "fabric-api", "mixinextras", "zakoopt");
	// Fabric's Mixin names merged handlers <kind>$<hash>$<mod id>$<method>
	private final Pattern HANDLER = Pattern.compile("^[a-zA-Z]+\\$[a-z0-9]+\\$([a-z][a-z0-9_-]*)\\$");
	private Map<Path, String> byPath;

	// the mod's display name, or null for the game, the loader, ZakoOpt itself and anything unknown
	public String owner(Class<?> type) {
		CodeSource source = type.getProtectionDomain().getCodeSource();
		if (source == null || source.getLocation() == null) {
			return null;
		}
		try {
			return name(paths().get(normalize(Path.of(source.getLocation().toURI()))));
		} catch (Exception e) {
			return null;
		}
	}

	public String owner(StackWalker.StackFrame frame) {
		String name = owner(frame.getDeclaringClass());
		if (name != null) {
			return name;
		}
		Matcher m = HANDLER.matcher(frame.getMethodName());
		return m.find() ? name(m.group(1)) : null;
	}

	private String name(String mod) {
		if (mod == null) {
			return null;
		}
		String id = Platform.loader().root(mod);
		return OURS.contains(id) || id.startsWith("fabric-") ? null : Platform.loader().name(id);
	}

	private Map<Path, String> paths() {
		if (byPath == null) {
			byPath = Platform.loader().paths();
		}
		return byPath;
	}

	private Path normalize(Path path) {
		return path.toAbsolutePath().normalize();
	}
}
