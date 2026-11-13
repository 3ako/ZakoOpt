package zako.opt;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// mixins into Sodium's own classes are added only when Sodium is installed; each is built only for some versions
public class MixinPlugin implements IMixinConfigPlugin {
	public static final boolean SODIUM = Platform.isModLoaded("sodium");
	public static final boolean VULKAN = Platform.isModLoaded("vulkanmod");
	// Polydium wraps the List.iterator() call our overwrite removes, and its config requires every injection
	public static final boolean POLYDIUM = Platform.isModLoaded("polydium");
	// AsyncParticles renders the particle queues itself, off the render thread
	public static final boolean ASYNC_PARTICLES = Platform.isModLoaded("asyncparticles");
	private static final List<String> PARTICLE_MIXINS = List.of("particle.ParticleEngineParallelMixin", "particle.QuadParticleGroupMixin");
	// Lunar is no Fabric mod: its launcher class is the tell
	public static final boolean LUNAR = MixinPlugin.class.getClassLoader().getResource("com/moonsworth/lunar/genesis/Genesis.class") != null;
	private static final List<String> LUNAR_MIXINS = List.of("gl.LunarMultiDrawMixin", "gl.LunarSodiumMultiDrawMixin", "block.BlockEntityRenderDispatcherCullMixin");
	private static final List<String> NO_LUNAR_MIXINS = List.of("block.BlockEntityRenderDispatcherCullWrapMixin");
	// NeoForge runs the same jar without Fabric Loader; the OFF mixins have no target there, the ONLY ones stand in for them
	public static final boolean NEOFORGE = MixinPlugin.class.getClassLoader().getResource("net/fabricmc/loader/api/FabricLoader.class") == null;
	private static final List<String> NEOFORGE_OFF = List.of("block.BlockEntityRenderDispatcherCullWrapMixin", "block.BlockEntityRenderDispatcherMixin", "block.BlockEntityRenderStatePoolMixin",
			"block.LevelRendererCrackTessMixin");
	private static final List<String> NEOFORGE_ONLY = List.of("block.NeoBlockEntityRenderDispatcherMixin", "block.NeoLevelRendererCrackMixin");
	public static final List<String> SODIUM_MIXINS = List.of("entity.SodiumEntityRendererMixin", "entity.SodiumBakedModelEncoderMixin", "gl.VertexConsumerUtilsMixin", "block.NonTerrainBlockRenderContextMixin", "block.SodiumChestMeshMixin", "block.SodiumSignMeshMixin",
			"gl.LunarSodiumMultiDrawMixin", "gl.SodiumOcclusionCullMixin", "gl.SodiumTerrainTimerMixin", "block.SodiumLeafCullMixin", "block.SodiumLeafCullOldMixin", "block.SodiumFarLeavesMixin", "gl.SodiumCubicSamplerMixin", "gl.SodiumSerializerCacheMixin");
	// Create's renderers, on NeoForge only: Create's Fabric port is not the code these were written against
	public static final boolean CREATE = NEOFORGE && Platform.isModLoaded("create");
	private static final List<String> CREATE_MIXINS = List.of("block.CreateFactoryPanelMixin", "block.CreateValueBoxMixin", "block.CreateBeltMixin", "block.CreatePackageAddressMixin", "block.CreateFarTickMixin", "block.CreateLevelTickersMixin", "block.CreateCouplingMixin", "block.CreateDoorMixin",
			"block.CreateBoundTickerAccessor", "block.CreateRebindableTickerAccessor", "block.CreateFrustumAccessor", "block.CreateBogeyAccessor", "block.CreateDoorAccessor");
	// Sodium writes model cubes its own fast way; ours stands in where it is missing
	private static final List<String> NO_SODIUM_MIXINS = List.of("entity.ModelPartFastMixin");
	// the immediate ring lives in OpenGL buffers; VulkanMod draws without them and rewrites RenderType.draw
	private static final List<String> RING_MIXINS = List.of("gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin", "gl.ByteBufferBuilderMixin", "gl.ByteBufferBuilderPoolMixin", "gl.RenderTypeDrawMixin");

	// a Sodium class only some releases have: its mixin would log a missing target on all the others
	private static final String OLD_LEAF_CULL_TARGET = "net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockOcclusionCache.class";

	private static boolean modsAdded;

	// every config uses this plugin: only the first one asked gets the Sodium and Create mixins
	@Override
	public List<String> getMixins() {
		if (modsAdded) {
			return null;
		}
		modsAdded = true;
		return Stream.concat(SODIUM ? SODIUM_MIXINS.stream().filter(m -> (LUNAR || !LUNAR_MIXINS.contains(m))
				&& (!m.equals("block.SodiumLeafCullOldMixin") || MixinPlugin.class.getClassLoader().getResource(OLD_LEAF_CULL_TARGET) != null)) : Stream.empty(),
				CREATE ? CREATE_MIXINS.stream() : Stream.empty()).filter(MixinPlugin::built).toList();
	}

	public static boolean built(String mixin) {
		return MixinPlugin.class.getClassLoader().getResource("zako/opt/mixin/" + mixin.replace('.', '/') + ".class") != null;
	}

	public static boolean applies(String mixin) {
		return built(mixin) && (SODIUM || !SODIUM_MIXINS.contains(mixin)) && (CREATE || !CREATE_MIXINS.contains(mixin)) && !(SODIUM && NO_SODIUM_MIXINS.contains(mixin)) && !(VULKAN && openGl(mixin))
				&& (LUNAR || !LUNAR_MIXINS.contains(mixin)) && !(LUNAR && NO_LUNAR_MIXINS.contains(mixin)) && !(NEOFORGE && NEOFORGE_OFF.contains(mixin)) && (NEOFORGE || !NEOFORGE_ONLY.contains(mixin)) && !(POLYDIUM && mixin.equals("entity.ModelResetPoseMixin"))
				&& !(ASYNC_PARTICLES && PARTICLE_MIXINS.contains(mixin));
	}

	// the HUD cache draws into its own OpenGL framebuffer, which flickers under VulkanMod; the chat batch is plain GuiGraphics;
	// GPU timers and the window blit call GL directly, and VulkanMod has no GL context to call; the render scale's swapped
	// main textures come out as stripes under VulkanMod even through the game's own API
	private static boolean openGl(String mixin) {
		return mixin.startsWith("gl.Gl") || RING_MIXINS.contains(mixin) || mixin.startsWith("hud.") && !mixin.equals("hud.ChatComponentBatchMixin")
				|| mixin.endsWith("TimerMixin") || mixin.equals("gl.RenderTargetBlitMixin")
				|| mixin.equals("gl.GameRendererScaleMixin");
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixin = mixinClassName.substring("zako.opt.mixin.".length());
		return !(VULKAN && openGl(mixin)) && !(SODIUM && NO_SODIUM_MIXINS.contains(mixin)) && (LUNAR || !LUNAR_MIXINS.contains(mixin))
				&& !(LUNAR && NO_LUNAR_MIXINS.contains(mixin)) && !(NEOFORGE && NEOFORGE_OFF.contains(mixin)) && (NEOFORGE || !NEOFORGE_ONLY.contains(mixin)) && !(POLYDIUM && mixin.equals("entity.ModelResetPoseMixin"))
				&& !(ASYNC_PARTICLES && PARTICLE_MIXINS.contains(mixin));
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		GuardWeaver.weave(targetClass, mixinClassName, mixinInfo);
	}
}
