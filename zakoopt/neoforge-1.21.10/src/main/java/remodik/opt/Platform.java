package remodik.opt;

import lombok.experimental.UtilityClass;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// the loader's view for the rest of the mod (NeoForge port: stands in for FabricLoader); the loading mod list is there
// before any mixin config is read, so this works from the mixin plugin too
@UtilityClass
public class Platform {
	public boolean isModLoaded(String id) {
		return mod(id).isPresent();
	}

	public Optional<IModInfo> mod(String id) {
		return mods().stream().filter(m -> m.getModId().equals(id)).<IModInfo>map(m -> m).findFirst();
	}

	public List<? extends IModInfo> mods() {
		return FMLLoader.getCurrent().getLoadingModList().getMods();
	}

	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public boolean isDevelopment() {
		FMLLoader loader = FMLLoader.getCurrentOrNull();
		return loader != null && !loader.isProduction();
	}
}
