package zako.opt;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.nio.file.Path;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// which installed mod some code belongs to, for naming it to the player; slow, meant for a one-off lookup when something is detected
@UtilityClass
public class Mods {
	private final Set<String> OURS = Set.of("minecraft", "java", "fabricloader", "fabric-api", "mixinextras", "zakoopt");
	// Fabric's Mixin names merged handlers <kind>$<hash>$<mod id>$<method>
	private final Pattern HANDLER = Pattern.compile("^[a-zA-Z]+\\$[a-z0-9]+\\$([a-z][a-z0-9_-]*)\\$");
	private Map<Path, ModContainer> byPath;

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
		return m.find() ? name(FabricLoader.getInstance().getModContainer(m.group(1)).orElse(null)) : null;
	}

	private String name(ModContainer mod) {
		if (mod == null) {
			return null;
		}
		for (Optional<ModContainer> parent = mod.getContainingMod(); parent.isPresent(); parent = mod.getContainingMod()) {
			mod = parent.get();
		}
		String id = mod.getMetadata().getId();
		return OURS.contains(id) || id.startsWith("fabric-") ? null : mod.getMetadata().getName();
	}

	private Map<Path, ModContainer> paths() {
		if (byPath == null) {
			byPath = new HashMap<>();
			for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
				if (mod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
					for (Path path : mod.getOrigin().getPaths()) {
						byPath.put(normalize(path), mod);
					}
				}
			}
		}
		return byPath;
	}

	private Path normalize(Path path) {
		return path.toAbsolutePath().normalize();
	}
}
