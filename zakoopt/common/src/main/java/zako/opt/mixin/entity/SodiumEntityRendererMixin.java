package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.CachedStack;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.immediate.model.EntityRenderer", remap = false)
public class SodiumEntityRendererMixin {
	// stackPush() looks the stack up through a ThreadLocal on every cuboid, and the render thread's map misses a lot.
	// WrapOperation, not Redirect: MixinExtras 0.5.4 (Fabric Loader 0.19.3) crashes wrapping a static @Redirect here
	@WrapOperation(method = "renderCuboid", at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private static MemoryStack zakoopt$cachedStack(Operation<MemoryStack> original) {
		return ZakoOptConfig.microOpts() ? CachedStack.push() : original.call();
	}
}
