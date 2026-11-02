package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.CrackCache;

// the crack loop of renderLevel, from its "destroyProgress" profiler section to "outline"; the tessellation call itself is
// taken by LevelRendererCrackTessMixin (Fabric) or NeoLevelRendererCrackMixin (NeoForge's ModelData overload)
@Mixin(LevelRenderer.class)
public class LevelRendererCrackMixin {
	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	// a crack's getBuffer switched the immediate buffer to its stage, an upload and a draw per block; queued cracks draw nothing here
	@WrapOperation(method = "renderLevel",
			slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"),
					to = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=outline")),
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer zakoopt$noSwitch(MultiBufferSource.BufferSource source, RenderType type, Operation<VertexConsumer> original) {
		return CrackCache.enabled() && CrackCache.hooked ? CrackCache.IDLE : original.call(source, type);
	}

	@Inject(method = "renderLevel",
			slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"),
					to = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=outline")),
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void zakoopt$flushCracks(CallbackInfo ci) {
		CrackCache.flush(renderBuffers.crumblingBufferSource());
	}

	// resource reloads rebuild the block models
	@Inject(method = "allChanged", at = @At("HEAD"))
	private void zakoopt$clearCracks(CallbackInfo ci) {
		CrackCache.clear();
	}
}
