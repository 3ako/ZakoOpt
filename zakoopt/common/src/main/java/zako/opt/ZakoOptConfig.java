package zako.opt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import zako.opt.gl.RingAutoTune;

@UtilityClass
@Slf4j(topic = "zakoopt")
public class ZakoOptConfig {
	public final String CHANNEL = "https://t.me/StarikZako";
	private final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
	// AxolotlClient draws its HUD straight to the screen, Exordium caches the HUD itself by swapping the main render target:
	// with our HUD cache either one flickers
	public final boolean FOREIGN_HUD = FabricLoader.getInstance().isModLoaded("axolotlclient") || FabricLoader.getInstance().isModLoaded("exordium");
	private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public final boolean RING_PRESENT = ZakoOptConfig.class.getClassLoader().getResource("zako/opt/gl/ImmediateRing.class") != null;
	private final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("zakoopt.json");
	private final int CORES = Runtime.getRuntime().availableProcessors();

	public Values values = load();
	private boolean guiAnimatedItems;
	private boolean frameFence;
	private boolean lazyClear;
	private boolean noEntitySort;
	private boolean bidiCache;
	private boolean preparedTextCache;
	private boolean itemLod;
	private int itemLodDistance;
	private boolean entityLod;
	private int entityLodDistance;
	private boolean playerLod;
	private boolean entityAnimFreeze;
	private int entityAnimDistance;
	private boolean particleLight;
	private boolean particlePhysics;
	private boolean particleLod;
	private boolean parallelParticles;
	private boolean parallelParticleVertices;
	private boolean spawnerCull;
	private int spawnerDistance;
	private boolean blockEntityCache;
	private boolean immediateRing;
	private boolean movingBlockFlatLight;
	private boolean movingBlockCache;
	private boolean lookupCaches;
	private boolean microOpts;
	private boolean microOpts2;
	private boolean microOpts3;
	private boolean tboCache;
	private boolean itemBounds;
	private boolean hudCache;
	private boolean skinAtlas;
	private boolean writerCache;
	private boolean signCache;
	private boolean fboShare;
	private boolean outlineSkip;
	private boolean ringZeroCopy;
	private boolean spawnerTickCache;
	private boolean spawnerReplay;

	static {
		refresh();
	}

	public final class Values {
		public boolean guiAnimatedItems = true;
		public boolean frameFence = true;
		public boolean lazyClear = true;
		public boolean noEntitySort = true;
		public boolean bidiCache = true;
		public boolean preparedTextCache = true;
		public boolean itemLod = true;
		public int itemLodDistance = 8;
		public boolean entityLod = true;
		public int entityLodDistance = 35;
		public boolean playerLod = true;
		public boolean entityAnimFreeze = false;
		public int entityAnimDistance = 16;
		public boolean particleLight = true;
		public boolean particlePhysics = true;
		public boolean particleLod = true;
		public boolean parallelParticles = true;
		public boolean spawnerCull = true;
		public int spawnerDistance = 16;
		public boolean spawnerTickCache = true;
		public boolean spawnerReplay = true;
		public boolean movingBlockFlatLight = true;
		public boolean movingBlockCache = true;
		public boolean lookupCaches = true;
		public boolean microOpts = true;
		public boolean hudCache = true;
		public boolean skinAtlas = true;
		public boolean writerCache = true;
		public boolean signCache = true;
		public boolean fboShare = true;
		public boolean outlineSkip = true;
		public boolean ringZeroCopy = true;
		public boolean immediateRing = true;
		public boolean blockEntityCache = true;
		public boolean updateCheck = true;
	}

	private Values load() {
		if (Files.exists(FILE)) {
			try {
				Values v = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Values.class);
				if (v != null) {
					return v;
				}
			} catch (Exception e) {
				log.warn("Can't read {}, using defaults", FILE, e);
			}
		}
		Values v = new Values();
		try {
			Files.writeString(FILE, GSON.toJson(v), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Can't save {}", FILE, e);
		}
		return v;
	}

	public void save() {
		refresh();
		try {
			Files.writeString(FILE, GSON.toJson(values), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Can't save {}", FILE, e);
		}
	}

	// -Dzakoopt.<key> overrides the config file; the benchmark toggles these at runtime
	private boolean on(String key, boolean value) {
		if ("false".equals(System.getProperty("zakoopt.all"))) {
			return false;
		}
		String p = System.getProperty("zakoopt." + key);
		return p == null ? value : Boolean.parseBoolean(p);
	}

	private int num(String key, int value) {
		String p = System.getProperty("zakoopt." + key);
		return p == null ? value : (int) Double.parseDouble(p);
	}

	// "auto" (and the config switch) only parallelize with 6+ logical CPUs: on 4 the render thread, integrated server and
	// Sodium's chunk builders already fill them, helpers get preempted and the render thread spins waiting; "true" forces it
	private boolean resolveParallelParticles() {
		if ("false".equals(System.getProperty("zakoopt.all"))) {
			return false;
		}
		String p = System.getProperty("zakoopt.parallelparticles");
		if (p == null) {
			return values.parallelParticles && CORES >= 6;
		}
		return p.equals("true") || (p.equals("auto") && CORES >= 6);
	}

	// System.getProperty hashes the key on every call, too slow for hot paths; resolved once per frame instead
	public void refresh() {
		guiAnimatedItems = on("guianim", values.guiAnimatedItems);
		frameFence = on("framefence", values.frameFence);
		lazyClear = on("lazyclear", values.lazyClear);
		noEntitySort = on("nosort", values.noEntitySort);
		bidiCache = on("textcache", values.bidiCache);
		preparedTextCache = on("preparedtext", values.preparedTextCache);
		itemLod = on("itemlod", values.itemLod);
		itemLodDistance = num("itemlod.distance", values.itemLodDistance);
		entityLod = on("entitylod", values.entityLod);
		entityLodDistance = num("entitylod.distance", values.entityLodDistance);
		playerLod = on("playerlod", values.playerLod);
		entityAnimFreeze = on("entityanim", values.entityAnimFreeze);
		entityAnimDistance = num("entityanim.distance", values.entityAnimDistance);
		particleLight = on("particlelight", values.particleLight);
		particlePhysics = on("particlephysics", values.particlePhysics);
		particleLod = on("particlelod", values.particleLod);
		parallelParticles = resolveParallelParticles();
		parallelParticleVertices = parallelParticles() && on("parallelvertices", true);
		spawnerCull = on("spawner", values.spawnerCull);
		spawnerDistance = num("spawner.distance", values.spawnerDistance);
		blockEntityCache = on("becache", values.blockEntityCache);
		boolean ringForced = System.getProperty("zakoopt.ring") != null;
		if (values.immediateRing && !ringForced && RING_PRESENT) {
			RingAutoTune.frame();
		}
		immediateRing = on("ring", values.immediateRing) && (ringForced || RingAutoTune.ringAllowed());
		movingBlockFlatLight = on("movingblock", values.movingBlockFlatLight);
		movingBlockCache = on("movingcache", values.movingBlockCache);
		lookupCaches = on("lookups", values.lookupCaches);
		microOpts = on("micro", values.microOpts);
		microOpts2 = on("micro2", values.microOpts);
		microOpts3 = on("micro3", values.microOpts);
		tboCache = on("tbo", values.microOpts);
		itemBounds = on("itembounds", values.microOpts);
		hudCache = on("hud", values.hudCache) && !FOREIGN_HUD;
		skinAtlas = on("skinatlas", values.skinAtlas);
		writerCache = on("writercache", values.writerCache);
		signCache = on("signcache", values.signCache);
		fboShare = on("fboshare", values.fboShare);
		outlineSkip = on("outlineskip", values.outlineSkip);
		// Iris reads vertices back to build its extended attributes, and reads from the mapped ring are uncached
		ringZeroCopy = on("zerocopy", values.ringZeroCopy) && !(IRIS && IrisApi.getInstance().isShaderPackInUse());
		spawnerTickCache = on("spawnertick", values.spawnerTickCache);
		spawnerReplay = on("spawnerreplay", values.spawnerReplay);
	}

	public boolean guiAnimatedItems() {
		return guiAnimatedItems;
	}

	public boolean frameFence() {
		return frameFence;
	}

	public boolean lazyClear() {
		return lazyClear;
	}

	public boolean noEntitySort() {
		return noEntitySort;
	}

	public boolean bidiCache() {
		return bidiCache;
	}

	public boolean preparedTextCache() {
		return preparedTextCache;
	}

	public boolean itemLod() {
		return itemLod;
	}

	public int itemLodDistance() {
		return itemLodDistance;
	}

	public boolean entityLod() {
		return entityLod;
	}

	public int entityLodDistance() {
		return entityLodDistance;
	}

	public boolean playerLod() {
		return playerLod;
	}

	public boolean entityAnimFreeze() {
		return entityAnimFreeze;
	}

	public int entityAnimDistance() {
		return entityAnimDistance;
	}

	public boolean particleLight() {
		return particleLight;
	}

	public boolean particlePhysics() {
		return particlePhysics;
	}

	public boolean particleLod() {
		return particleLod;
	}

	public boolean parallelParticles() {
		return parallelParticles;
	}

	public boolean parallelParticleVertices() {
		return parallelParticleVertices;
	}

	public boolean spawnerCull() {
		return spawnerCull;
	}

	public int spawnerDistance() {
		return spawnerDistance;
	}

	public boolean blockEntityCache() {
		return blockEntityCache;
	}

	public boolean immediateRing() {
		return immediateRing;
	}

	public boolean movingBlockFlatLight() {
		return movingBlockFlatLight;
	}

	public boolean movingBlockCache() {
		return movingBlockCache;
	}

	public boolean lookupCaches() {
		return lookupCaches;
	}

	public boolean microOpts() {
		return microOpts;
	}

	public boolean microOpts2() {
		return microOpts2;
	}

	public boolean microOpts3() {
		return microOpts3;
	}

	public boolean tboCache() {
		return tboCache;
	}

	public boolean itemBounds() {
		return itemBounds;
	}

	public boolean hudCache() {
		return hudCache;
	}

	public boolean skinAtlas() {
		return skinAtlas;
	}

	public boolean writerCache() {
		return writerCache;
	}

	public boolean signCache() {
		return signCache;
	}

	public boolean fboShare() {
		return fboShare;
	}

	public boolean outlineSkip() {
		return outlineSkip;
	}

	public boolean ringZeroCopy() {
		return ringZeroCopy;
	}

	public boolean spawnerTickCache() {
		return spawnerTickCache;
	}

	public boolean spawnerReplay() {
		return spawnerReplay;
	}
}
