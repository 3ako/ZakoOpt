package zako.opt;

import lombok.experimental.UtilityClass;
import zako.opt.platform.FabricPlatform;
import zako.opt.platform.Loader;

import java.nio.file.Path;

// the mod loader behind one door, so the rest of the mod names no loader's API; picked once, when first asked
@UtilityClass
public class Platform {
	// a jar without Fabric Loader gets the NeoForge one, which only the 26.x modules carry; looked up by name so the 1.21.x ones still link
	private final Loader LOADER = Platform.class.getClassLoader().getResource("net/fabricmc/loader/api/FabricLoader.class") != null
			? new FabricPlatform() : neoForge();

	private Loader neoForge() {
		try {
			return (Loader) Class.forName("zako.opt.platform.NeoPlatform").getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("ZakoOpt: no supported mod loader (Fabric) found", e);
		}
	}

	public Loader loader() {
		return LOADER;
	}

	public boolean isModLoaded(String id) {
		return LOADER.isModLoaded(id);
	}

	public Path configDir() {
		return LOADER.configDir();
	}

	public boolean isDevelopment() {
		return LOADER.isDevelopment();
	}
}
