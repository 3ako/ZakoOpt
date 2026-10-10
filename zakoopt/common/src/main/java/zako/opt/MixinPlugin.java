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
	public static final List<String> SODIUM_MIXINS = List.of("entity.SodiumEntityRendererMixin", "gl.VertexConsumerUtilsMixin", "block.NonTerrainBlockRenderContextMixin");
	// the immediate ring lives in OpenGL buffers; VulkanMod draws without them and rewrites RenderType.draw
	private static final List<String> RING_MIXINS = List.of("gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin", "gl.ByteBufferBuilderMixin", "gl.ByteBufferBuilderPoolMixin", "gl.RenderTypeDrawMixin");

	private static boolean sodiumAdded;

	// every config uses this plugin: only the first one asked gets the Sodium mixins
	@Override
	public List<String> getMixins() {
		if (!SODIUM || sodiumAdded) {
			return null;
		}
		sodiumAdded = true;
		return SODIUM_MIXINS.stream().filter(MixinPlugin::built).toList();
	}

	public static boolean built(String mixin) {
		return MixinPlugin.class.getClassLoader().getResource("zako/opt/mixin/" + mixin.replace('.', '/') + ".class") != null;
	}

	public static boolean applies(String mixin) {
		return built(mixin) && (SODIUM || !SODIUM_MIXINS.contains(mixin)) && !(VULKAN && openGl(mixin));
	}

	private static boolean openGl(String mixin) {
		return mixin.startsWith("gl.Gl") || RING_MIXINS.contains(mixin);
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return !(VULKAN && openGl(mixinClassName.substring("zako.opt.mixin.".length())));
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
