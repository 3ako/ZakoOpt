package zako.opt.platform;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.locating.IModFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// the loading mod list, not ModList: the mixin plugin asks before NeoForge builds ModList
public final class NeoPlatform implements Loader {
	private Optional<ModInfo> mod(String id) {
		return FMLLoader.getLoadingModList().getMods().stream().filter(mod -> mod.getModId().equals(id)).findFirst();
	}

	@Override
	public boolean isModLoaded(String id) {
		return mod(id).isPresent();
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isDevelopment() {
		return !FMLLoader.isProduction();
	}

	@Override
	public String version(String id) {
		return mod(id).map(mod -> mod.getVersion().toString()).orElse(null);
	}

	@Override
	public String name(String id) {
		return mod(id).map(ModInfo::getDisplayName).orElse(null);
	}

	@Override
	public String root(String id) {
		Optional<ModInfo> mod = mod(id);
		if (mod.isEmpty()) {
			return id;
		}
		IModFile file = mod.get().getOwningFile().getFile();
		while (file.getDiscoveryAttributes().parent() != null) {
			file = file.getDiscoveryAttributes().parent();
		}
		return file.getModInfos().isEmpty() ? id : file.getModInfos().getFirst().getModId();
	}

	@Override
	public Map<Path, String> paths() {
		Map<Path, String> paths = new HashMap<>();
		for (ModFileInfo file : FMLLoader.getLoadingModList().getModFiles()) {
			if (!file.getMods().isEmpty()) {
				paths.put(file.getFile().getFilePath().toAbsolutePath().normalize(), file.getMods().getFirst().getModId());
			}
		}
		return paths;
	}
}
