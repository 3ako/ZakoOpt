package zako.opt.feature;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.experimental.UtilityClass;
import net.irisshaders.iris.api.v0.IrisApi;
import zako.opt.ZakoOptConfig;
import zako.opt.feature.blocks.*;
import zako.opt.feature.items.*;
import zako.opt.feature.particles.*;
import zako.opt.feature.render.*;
import zako.opt.feature.spawners.*;
import zako.opt.feature.ui.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// every feature of the mod, in the order the options screens list them within their groups
@UtilityClass
public class Features {
	private final List<Feature> LIST = new ArrayList<>();

	public final CorePinFeature CORE_PIN = add(new CorePinFeature());
	public final FrameFenceFeature FRAME_FENCE = add(new FrameFenceFeature());
	public final LunarMultiDrawFeature LUNAR_MULTIDRAW = add(new LunarMultiDrawFeature());
	public final LazyClearFeature LAZY_CLEAR = add(new LazyClearFeature());
	public final NoEntitySortFeature NO_ENTITY_SORT = add(new NoEntitySortFeature());
	public final NoItemSortFeature NO_ITEM_SORT = add(new NoItemSortFeature());
	public final SkinAtlasFeature SKIN_ATLAS = add(new SkinAtlasFeature());
	public final WriterCacheFeature WRITER_CACHE = add(new WriterCacheFeature());
	public final FboShareFeature FBO_SHARE = add(new FboShareFeature());
	public final OutlineSkipFeature OUTLINE_SKIP = add(new OutlineSkipFeature());
	public final DisplayCullFeature DISPLAY_CULL = add(new DisplayCullFeature());
	public final ImmediateRingFeature IMMEDIATE_RING = add(new ImmediateRingFeature());
	public final RingZeroCopyFeature RING_ZERO_COPY = add(new RingZeroCopyFeature());
	public final FastBlitFeature FAST_BLIT = add(new FastBlitFeature());
	public final OcclusionCullFeature OCCLUSION_CULL = add(new OcclusionCullFeature());

	public final ItemLodFeature ITEM_LOD = add(new ItemLodFeature());
	public final ItemSingleCopyFeature ITEM_SINGLE_COPY = add(new ItemSingleCopyFeature());
	public final RestingItemsFeature RESTING_ITEMS = add(new RestingItemsFeature());
	public final ParallelModelsFeature PARALLEL_MODELS = add(new ParallelModelsFeature());
	public final StatePoolFeature STATE_POOL = add(new StatePoolFeature());
	public final EntityLodFeature ENTITY_LOD = add(new EntityLodFeature());
	public final PlayerLodFeature PLAYER_LOD = add(new PlayerLodFeature());
	public final EntityAnimFreezeFeature ENTITY_ANIM_FREEZE = add(new EntityAnimFreezeFeature());

	public final SpawnerCullFeature SPAWNER_CULL = add(new SpawnerCullFeature());
	public final SpawnerTickCacheFeature SPAWNER_TICK_CACHE = add(new SpawnerTickCacheFeature());
	public final SpawnerReplayFeature SPAWNER_REPLAY = add(new SpawnerReplayFeature());

	public final LeafCullFeature LEAF_CULL = add(new LeafCullFeature());
	public final FarLeavesFeature FAR_LEAVES = add(new FarLeavesFeature());
	public final MovingBlockFlatLightFeature MOVING_BLOCK_FLAT_LIGHT = add(new MovingBlockFlatLightFeature());
	public final MovingBlockCacheFeature MOVING_BLOCK_CACHE = add(new MovingBlockCacheFeature());
	public final BlockEntityCacheFeature BLOCK_ENTITY_CACHE = add(new BlockEntityCacheFeature());
	public final BlockEntityEarlyCullFeature BLOCK_ENTITY_EARLY_CULL = add(new BlockEntityEarlyCullFeature());
	public final SignCacheFeature SIGN_CACHE = add(new SignCacheFeature());
	public final LookupCachesFeature LOOKUP_CACHES = add(new LookupCachesFeature());
	public final MicroOptsFeature MICRO_OPTS = add(new MicroOptsFeature());

	public final ParticleLightFeature PARTICLE_LIGHT = add(new ParticleLightFeature());
	public final ParticlePhysicsFeature PARTICLE_PHYSICS = add(new ParticlePhysicsFeature());
	public final ParticleLodFeature PARTICLE_LOD = add(new ParticleLodFeature());
	public final ParallelParticlesFeature PARALLEL_PARTICLES = add(new ParallelParticlesFeature());

	public final HudCacheFeature HUD_CACHE = add(new HudCacheFeature());
	public final GuiAnimatedItemsFeature GUI_ANIMATED_ITEMS = add(new GuiAnimatedItemsFeature());
	public final BidiCacheFeature BIDI_CACHE = add(new BidiCacheFeature());
	public final PreparedTextCacheFeature PREPARED_TEXT_CACHE = add(new PreparedTextCacheFeature());
	public final UpdateCheckFeature UPDATE_CHECK = add(new UpdateCheckFeature());

	// an array for the per-frame loop
	private final Feature[] ALL = LIST.toArray(new Feature[0]);

	private <T extends Feature> T add(T feature) {
		LIST.add(feature);
		return feature;
	}

	public List<Feature> all() {
		return List.of(ALL);
	}

	public Feature byKey(String key) {
		for (Feature f : ALL) {
			if (f.key().equals(key)) {
				return f;
			}
		}
		return null;
	}

	// the master switch and -Dzakoopt.all=false turn every feature off at once; resolved once per refresh
	public boolean active;

	public boolean shaders() {
		return ZakoOptConfig.IRIS && IrisApi.getInstance().isShaderPackInUse();
	}

	// set by features whose change needs the terrain meshed again: one rebuild for all of them, the master switch flips several
	public boolean terrainChanged;

	public void refresh() {
		active = ZakoOptConfig.enabled && !"false".equals(System.getProperty("zakoopt.all"));
		for (Feature f : ALL) {
			f.refresh();
		}
		if (terrainChanged) {
			terrainChanged = false;
			zako.opt.block.Terrain.rebuild();
		}
	}

	// every value the player can set, by option key: the switches and their settings
	public Map<String, Object> snapshot() {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Feature f : ALL) {
			values.put(f.key(), f.value);
			for (Field s : f.settings()) {
				values.put(s.getAnnotation(Setting.class).key(), f.get(s));
			}
		}
		return values;
	}

	public void restore(Map<String, Object> values) {
		for (Feature f : ALL) {
			f.value = (Boolean) values.getOrDefault(f.key(), f.value);
			for (Field s : f.settings()) {
				f.set(s, values.getOrDefault(s.getAnnotation(Setting.class).key(), f.get(s)));
			}
		}
	}

	// what a fresh install has
	@lombok.SneakyThrows(ReflectiveOperationException.class)
	public Map<String, Object> defaults() {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Feature f : ALL) {
			Feature fresh = f.getClass().getDeclaredConstructor().newInstance();
			values.put(f.key(), fresh.value);
			for (Field s : fresh.settings()) {
				values.put(s.getAnnotation(Setting.class).key(), fresh.get(s));
			}
		}
		return values;
	}

	// a setting given as -Dzakoopt.<key> at launch holds for the session; the config keeps what the player set
	private final Map<String, Object> LAUNCH_OVERRIDDEN = new LinkedHashMap<>();

	// zakoopt.json keeps the field names the config had before features were classes
	public void read(JsonObject json) {
		for (Feature f : ALL) {
			String name = config(f.data().config(), f.key());
			if (json.has(name)) {
				f.value = json.get(name).getAsBoolean();
			}
			for (Field s : f.settings()) {
				Setting setting = s.getAnnotation(Setting.class);
				String field = config(setting.config(), setting.key());
				if (json.has(field)) {
					f.set(s, s.getType() == int.class ? (Object) json.get(field).getAsInt() : (Object) json.get(field).getAsBoolean());
				}
				String launch = System.getProperty("zakoopt." + setting.key());
				if (launch != null) {
					LAUNCH_OVERRIDDEN.put(setting.key(), f.get(s));
					f.set(s, s.getType() == int.class ? (Object) Integer.parseInt(launch) : (Object) Boolean.parseBoolean(launch));
				}
			}
		}
	}

	public void write(JsonObject json) {
		for (Feature f : ALL) {
			json.addProperty(config(f.data().config(), f.key()), f.value);
			for (Field s : f.settings()) {
				Setting setting = s.getAnnotation(Setting.class);
				Object value = LAUNCH_OVERRIDDEN.getOrDefault(setting.key(), f.get(s));
				json.add(config(setting.config(), setting.key()), value instanceof Number n ? new JsonPrimitive(n) : new JsonPrimitive((Boolean) value));
			}
		}
	}

	private String config(String explicit, String key) {
		if (!explicit.isEmpty()) {
			return explicit;
		}
		StringBuilder s = new StringBuilder();
		boolean upper = false;
		for (char c : key.toCharArray()) {
			if (c == '_') {
				upper = true;
			} else {
				s.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			}
		}
		return s.toString();
	}
}
