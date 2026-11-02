package zako.opt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import zako.opt.feature.Features;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@UtilityClass
@Slf4j(topic = "zakoopt")
public class ZakoOptConfig {
	public final boolean IRIS = Platform.isModLoaded("iris");
	// mods that draw the HUD their own way; Soup Visual renders the extracted HUD itself mid-extraction, so the cache's texture
	// stayed empty and the HUD blinked
	public final String FOREIGN_HUD_MOD = java.util.stream.Stream.of("axolotlclient", "exordium", "gnetum", "soupapi")
			.map(Platform.loader()::name).filter(java.util.Objects::nonNull).findFirst().orElse(null);
	public final boolean FOREIGN_HUD = FOREIGN_HUD_MOD != null;
	private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public final boolean RING_PRESENT = ZakoOptConfig.class.getClassLoader().getResource("zako/opt/gl/ImmediateRing.class") != null && !MixinPlugin.VULKAN;
	private final Path FILE = Platform.configDir().resolve("zakoopt.json");
	public final int CORES = Runtime.getRuntime().availableProcessors();
	// the whole mod's effect, for comparing against plain play; the features keep their values
	public boolean enabled = true;
	public boolean welcomed;

	static {
		load();
		refresh();
	}

	private void load() {
		if (Files.exists(FILE)) {
			try {
				JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
				Features.read(json);
				enabled = Features.jsonBoolean(json, "enabled", enabled);
				welcomed = Features.jsonBoolean(json, "welcomed", welcomed);
				return;
			} catch (Exception e) {
				// the file is the player's, not ours to overwrite: it goes aside and this session runs on the defaults
				log.warn("Can't read {}, starting from the defaults", FILE, e);
				moveAside();
				return;
			}
		}
		save();
	}

	// keeps a config we could not read instead of throwing it away, and leaves the next save a clean one to write
	private void moveAside() {
		Path aside = FILE.resolveSibling(FILE.getFileName() + ".broken");
		try {
			Files.move(FILE, aside, StandardCopyOption.REPLACE_EXISTING);
			log.warn("The old config is at {}", aside);
		} catch (IOException e) {
			log.error("Can't move {} aside", FILE, e);
		}
	}

	public void save() {
		refresh();
		JsonObject json = new JsonObject();
		Features.write(json);
		json.addProperty("enabled", enabled);
		json.addProperty("welcomed", welcomed);
		Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
		try {
			// written whole next to it and moved into place, so a crash or a full disk mid-write leaves the old file readable
			Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.error("Can't save {}", FILE, e);
		}
	}

	// System.getProperty hashes the key on every call, too slow for hot paths; resolved once per frame instead
	public void refresh() {
		Features.refresh();
		Guard.tell();
	}
}
