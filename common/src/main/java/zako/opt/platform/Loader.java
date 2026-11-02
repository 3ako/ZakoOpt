package zako.opt.platform;

import java.nio.file.Path;
import java.util.Map;

// what the mod asks of its mod loader; Platform picks the implementation for the loader the game runs on
public interface Loader {
	boolean isModLoaded(String id);

	Path configDir();

	// a dev run from Gradle
	boolean isDevelopment();

	// the mod's version as its metadata spells it, or null when it is not installed
	String version(String id);

	// the mod's display name, or null when it is not installed
	String name(String id);

	// the id of the mod that ships this one inside its jar, or the id itself when it stands alone
	String root(String id);

	// every mod loaded from a file or folder, by that path (absolute and normalized) to its id; slow, for one-off lookups
	Map<Path, String> paths();
}
