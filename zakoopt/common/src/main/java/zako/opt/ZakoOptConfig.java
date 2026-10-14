package zako.opt;

import java.net.URI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import zako.opt.feature.Features;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
@Slf4j(topic = "zakoopt")
public class ZakoOptConfig {
	public final URI CHANNEL = URI.create("https://t.me/StarikZako");
	public final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
	// mods that draw the HUD their own way; Soup Visual renders the extracted HUD itself mid-extraction, so the cache's texture
	// stayed empty and the HUD blinked
	public final String FOREIGN_HUD_MOD = java.util.stream.Stream.of("axolotlclient", "exordium", "gnetum", "soupapi")
			.flatMap(id -> FabricLoader.getInstance().getModContainer(id).stream())
			.map(mod -> mod.getMetadata().getName()).findFirst().orElse(null);
	public final boolean FOREIGN_HUD = FOREIGN_HUD_MOD != null;
	private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public final boolean RING_PRESENT = ZakoOptConfig.class.getClassLoader().getResource("zako/opt/gl/ImmediateRing.class") != null && !MixinPlugin.VULKAN;
	private final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zakoopt.json");
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
				if (json.has("enabled")) {
					enabled = json.get("enabled").getAsBoolean();
				}
				if (json.has("welcomed")) {
					welcomed = json.get("welcomed").getAsBoolean();
				}
				return;
			} catch (Exception e) {
				log.warn("Can't read {}, using defaults", FILE, e);
			}
		}
		save();
	}

	public void save() {
		refresh();
		JsonObject json = new JsonObject();
		Features.write(json);
		json.addProperty("enabled", enabled);
		json.addProperty("welcomed", welcomed);
		try {
			Files.writeString(FILE, GSON.toJson(json), StandardCharsets.UTF_8);
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
