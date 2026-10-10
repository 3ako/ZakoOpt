package zako.opt;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
public class ZakoOptions {
	public final String VERSION = FabricLoader.getInstance().getModContainer("zakoopt").orElseThrow().getMetadata().getVersion().getFriendlyString();

	public enum Impact { LOW, MEDIUM, HIGH }

	public sealed interface Option permits Toggle, Distance {
		String key();

		String[] requires();

		// an option is shown only if one of the mixins behind it is built for this Minecraft version and applied
		default boolean available() {
			return requires().length == 0 || Arrays.stream(requires())
					.anyMatch(MixinPlugin::applies);
		}
	}

	public record Toggle(String key, Impact impact, Consumer<Boolean> set, Supplier<Boolean> get, String... requires) implements Option {
	}

	public record Distance(String key, int min, int max, Consumer<Integer> set, Supplier<Integer> get, String... requires) implements Option {
	}

	public record Group(String key, List<Option> options) {
		public List<Option> available() {
			return options.stream().filter(Option::available).toList();
		}
	}

	public record Page(String key, List<Group> groups) {
	}

	public final List<Page> PAGES = pages(ZakoOptConfig.values);
	private final Set<String> RECOMMENDED = toggles(pages(new ZakoOptConfig.Values())).stream().filter(t -> t.get().get()).map(Toggle::key).collect(Collectors.toSet());

	public boolean recommended(String key) {
		return RECOMMENDED.contains(key);
	}

	public List<Toggle> recommended() {
		return toggles(PAGES).stream().filter(t -> recommended(t.key()) && t.available()).toList();
	}

	private List<Toggle> toggles(List<Page> pages) {
		return pages.stream().flatMap(p -> p.groups().stream()).flatMap(g -> g.options().stream())
				.filter(Toggle.class::isInstance).map(Toggle.class::cast).toList();
	}

	private List<Page> pages(ZakoOptConfig.Values v) {
		return List.of(
				new Page("render", List.of(
						new Group("render", List.of(
								new Toggle("frame_fence", Impact.HIGH, b -> v.frameFence = b, () -> v.frameFence, "gl.GlCommandEncoderMixin"),
								new Toggle("lazy_clear", Impact.LOW, b -> v.lazyClear = b, () -> v.lazyClear, "gl.GlCommandEncoderClearMixin"),
								new Toggle("no_entity_sort", Impact.LOW, b -> v.noEntitySort = b, () -> v.noEntitySort, "gl.BufferSourceMixin"),
								new Toggle("skin_atlas", Impact.MEDIUM, b -> v.skinAtlas = b, () -> v.skinAtlas, "entity.LivingEntityRendererSkinMixin"),
								new Toggle("writer_cache", Impact.LOW, b -> v.writerCache = b, () -> v.writerCache, "gl.VertexConsumerUtilsMixin"),
								new Toggle("fbo_share", Impact.LOW, b -> v.fboShare = b, () -> v.fboShare, "gl.GlCommandEncoderDepthClearMixin", "gl.GlTextureViewMixin"),
								new Toggle("outline_skip", Impact.LOW, b -> v.outlineSkip = b, () -> v.outlineSkip, "entity.LevelRendererOutlineMixin"),
								new Toggle("immediate_ring", Impact.MEDIUM, b -> v.immediateRing = b, () -> v.immediateRing, "gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"),
								new Toggle("ring_zero_copy", Impact.MEDIUM, b -> v.ringZeroCopy = b, () -> v.ringZeroCopy, "gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"))),
						new Group("items", List.of(
								new Toggle("item_lod", Impact.HIGH, b -> v.itemLod = b, () -> v.itemLod, "entity.ItemRendererMixin", "entity.ItemFeatureRendererMixin"),
								new Distance("item_lod_distance", 4, 48, i -> v.itemLodDistance = i, () -> v.itemLodDistance, "entity.ItemRendererMixin", "entity.ItemFeatureRendererMixin"),
								new Toggle("entity_lod", Impact.HIGH, b -> v.entityLod = b, () -> v.entityLod, "entity.LivingEntityLodMixin", "entity.AvatarLodMixin", "entity.PlayerLodMixin"),
								new Distance("entity_lod_distance", 8, 96, i -> v.entityLodDistance = i, () -> v.entityLodDistance, "entity.LivingEntityLodMixin", "entity.AvatarLodMixin", "entity.PlayerLodMixin"),
								new Toggle("player_lod", Impact.MEDIUM, b -> v.playerLod = b, () -> v.playerLod, "entity.AvatarLodMixin", "entity.PlayerLodMixin"),
								new Toggle("entity_anim_freeze", Impact.HIGH, b -> v.entityAnimFreeze = b, () -> v.entityAnimFreeze, "entity.ModelFeatureRendererMixin", "entity.LivingEntityAnimFreezeMixin"),
								new Distance("entity_anim_distance", 4, 96, i -> v.entityAnimDistance = i, () -> v.entityAnimDistance, "entity.ModelFeatureRendererMixin", "entity.LivingEntityAnimFreezeMixin"))),
						new Group("spawners", List.of(
								new Toggle("spawner_cull", Impact.HIGH, b -> v.spawnerCull = b, () -> v.spawnerCull, "block.SpawnerRendererMixin"),
								new Distance("spawner_distance", 4, 64, i -> v.spawnerDistance = i, () -> v.spawnerDistance, "block.SpawnerRendererMixin"),
								new Toggle("spawner_tick_cache", Impact.MEDIUM, b -> v.spawnerTickCache = b, () -> v.spawnerTickCache, "block.SpawnerRendererMixin"),
								new Toggle("spawner_replay", Impact.MEDIUM, b -> v.spawnerReplay = b, () -> v.spawnerReplay, "block.SpawnerRendererMixin"))),
						new Group("blocks", List.of(
								new Toggle("moving_block_flat_light", Impact.HIGH, b -> v.movingBlockFlatLight = b, () -> v.movingBlockFlatLight, "block.NonTerrainBlockRenderContextMixin"),
								new Toggle("moving_block_cache", Impact.HIGH, b -> v.movingBlockCache = b, () -> v.movingBlockCache, "block.BlockFeatureRendererMixin"),
								new Toggle("block_entity_cache", Impact.LOW, b -> v.blockEntityCache = b, () -> v.blockEntityCache, "block.ChestRendererMixin"),
								new Toggle("sign_cache", Impact.LOW, b -> v.signCache = b, () -> v.signCache, "block.AbstractSignRendererMixin"),
								new Toggle("lookup_caches", Impact.LOW, b -> v.lookupCaches = b, () -> v.lookupCaches, "gl.RenderTypesMixin", "block.PistonHeadRendererMixin"),
								new Toggle("micro_opts", Impact.LOW, b -> v.microOpts = b, () -> v.microOpts))))),
				new Page("particles", List.of(
						new Group("particles", List.of(
								new Toggle("particle_light", Impact.MEDIUM, b -> v.particleLight = b, () -> v.particleLight, "particle.ParticleMixin"),
								new Toggle("particle_physics", Impact.HIGH, b -> v.particlePhysics = b, () -> v.particlePhysics, "particle.ParticleMixin"),
								new Toggle("particle_lod", Impact.MEDIUM, b -> v.particleLod = b, () -> v.particleLod, "particle.QuadParticleGroupMixin", "particle.ParticleEngineLodMixin"),
								new Toggle("parallel_particles", Impact.HIGH, b -> v.parallelParticles = b, () -> v.parallelParticles, "particle.QuadParticleGroupMixin", "particle.ParticleEngineParallelMixin"))))),
				new Page("interface", List.of(
						new Group("interface", List.of(
								new Toggle("hud_cache", Impact.HIGH, b -> v.hudCache = b, () -> v.hudCache, "hud.GuiRendererMixin", "hud.GuiRendererTargetMixin", "hud.GuiHudMixin"),
								new Toggle("gui_animated_items", Impact.MEDIUM, b -> v.guiAnimatedItems = b, () -> v.guiAnimatedItems, "hud.GuiRendererMixin", "hud.GuiItemAtlasMixin"),
								new Toggle("bidi_cache", Impact.LOW, b -> v.bidiCache = b, () -> v.bidiCache, "text.ClientLanguageMixin"),
								new Toggle("prepared_text_cache", Impact.MEDIUM, b -> v.preparedTextCache = b, () -> v.preparedTextCache, "text.FontMixin"),
								new Toggle("update_check", Impact.LOW, b -> v.updateCheck = b, () -> v.updateCheck))))));
	}
}
