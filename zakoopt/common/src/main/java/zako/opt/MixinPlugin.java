package zako.opt;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

// mixins into Sodium's own classes are added only when Sodium is installed; each is built only for some versions
public class MixinPlugin implements IMixinConfigPlugin {
	public static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
	public static final boolean VULKAN = FabricLoader.getInstance().isModLoaded("vulkanmod");
	// Lunar is no Fabric mod: its launcher class is the tell
	public static final boolean LUNAR = MixinPlugin.class.getClassLoader().getResource("com/moonsworth/lunar/genesis/Genesis.class") != null;
	private static final List<String> LUNAR_MIXINS = List.of("gl.LunarMultiDrawMixin", "gl.LunarSodiumMultiDrawMixin");
	public static final List<String> SODIUM_MIXINS = List.of("entity.SodiumEntityRendererMixin", "gl.VertexConsumerUtilsMixin", "block.NonTerrainBlockRenderContextMixin",
			"gl.LunarSodiumMultiDrawMixin", "gl.SodiumOcclusionCullMixin", "gl.SodiumTerrainTimerMixin", "block.SodiumLeafCullMixin", "block.SodiumLeafCullOldMixin", "block.SodiumFarLeavesMixin");
	// Sodium writes model cubes its own fast way; ours stands in where it is missing
	private static final List<String> NO_SODIUM_MIXINS = List.of("entity.ModelPartFastMixin");
	// the immediate ring lives in OpenGL buffers; VulkanMod draws without them and rewrites RenderType.draw
	private static final List<String> RING_MIXINS = List.of("gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin", "gl.ByteBufferBuilderMixin", "gl.ByteBufferBuilderPoolMixin", "gl.RenderTypeDrawMixin");

	// a Sodium class only some releases have: its mixin would log a missing target on all the others
	private static final String OLD_LEAF_CULL_TARGET = "net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockOcclusionCache.class";

	private static boolean sodiumAdded;

	// every config uses this plugin: only the first one asked gets the Sodium mixins
	@Override
	public List<String> getMixins() {
		if (!SODIUM || sodiumAdded) {
			return null;
		}
		sodiumAdded = true;
		return SODIUM_MIXINS.stream().filter(m -> built(m) && (LUNAR || !LUNAR_MIXINS.contains(m))
				&& (!m.equals("block.SodiumLeafCullOldMixin") || MixinPlugin.class.getClassLoader().getResource(OLD_LEAF_CULL_TARGET) != null)).toList();
	}

	public static boolean built(String mixin) {
		return MixinPlugin.class.getClassLoader().getResource("zako/opt/mixin/" + mixin.replace('.', '/') + ".class") != null;
	}

	public static boolean applies(String mixin) {
		return built(mixin) && (SODIUM || !SODIUM_MIXINS.contains(mixin)) && !(SODIUM && NO_SODIUM_MIXINS.contains(mixin)) && !(VULKAN && openGl(mixin))
				&& (LUNAR || !LUNAR_MIXINS.contains(mixin));
	}

	// the HUD cache draws into its own OpenGL framebuffer, which flickers under VulkanMod; the chat batch is plain GuiGraphics
	private static boolean openGl(String mixin) {
		return mixin.startsWith("gl.Gl") || RING_MIXINS.contains(mixin) || mixin.startsWith("hud.") && !mixin.equals("hud.ChatComponentBatchMixin");
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String mixin = mixinClassName.substring("zako.opt.mixin.".length());
		return !(VULKAN && openGl(mixin)) && !(SODIUM && NO_SODIUM_MIXINS.contains(mixin)) && (LUNAR || !LUNAR_MIXINS.contains(mixin));
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
	}
}
