package zako.opt.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FabricPlatform implements Loader {
	private final FabricLoader loader = FabricLoader.getInstance();

	@Override
	public boolean isModLoaded(String id) {
		return loader.isModLoaded(id);
	}

	@Override
	public Path configDir() {
		return loader.getConfigDir();
	}

	@Override
	public boolean isDevelopment() {
		return loader.isDevelopmentEnvironment();
	}

	@Override
	public String version(String id) {
		return loader.getModContainer(id).map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
	}

	@Override
	public String name(String id) {
		return loader.getModContainer(id).map(mod -> mod.getMetadata().getName()).orElse(null);
	}

	@Override
	public String root(String id) {
		Optional<ModContainer> mod = loader.getModContainer(id);
		if (mod.isEmpty()) {
			return id;
		}
		ModContainer top = mod.get();
		for (Optional<ModContainer> parent = top.getContainingMod(); parent.isPresent(); parent = top.getContainingMod()) {
			top = parent.get();
		}
		return top.getMetadata().getId();
	}

	@Override
	public Map<Path, String> paths() {
		Map<Path, String> paths = new HashMap<>();
		for (ModContainer mod : loader.getAllMods()) {
			if (mod.getOrigin().getKind() == ModOrigin.Kind.PATH) {
				for (Path path : mod.getOrigin().getPaths()) {
					paths.put(path.toAbsolutePath().normalize(), mod.getMetadata().getId());
				}
			}
		}
		return paths;
	}
}
