package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.CachedStack;

// same ThreadLocal lookup as SodiumEntityRendererMixin, once per item quad
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder", remap = false)
public class SodiumBakedModelEncoderMixin {
	@WrapOperation(method = "writeQuadVertices", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/system/MemoryStack;stackPush()Lorg/lwjgl/system/MemoryStack;"))
	private static MemoryStack zakoopt$cachedStack(Operation<MemoryStack> original) {
		return Features.MICRO_OPTS.on ? CachedStack.push() : original.call();
	}
}
