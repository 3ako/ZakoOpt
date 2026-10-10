package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.AnimFreeze;
import zako.opt.entity.ParallelModelHook;
import zako.opt.entity.StateEntity;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void zakoopt$parallel(SubmitNodeCollection collection, MultiBufferSource.BufferSource buffers, OutlineBufferSource outline,
								  MultiBufferSource.BufferSource crumbling, CallbackInfo ci) {
		if (ParallelModelHook.render((ModelFeatureRenderer) (Object) this, collection.getModelSubmits(), buffers, outline, crumbling)) {
			ci.cancel();
		}
	}

	@WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", ordinal = 0))
	private void zakoopt$frozenAnimation(Model<?> model, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, Operation<Void> original,
										 SubmitNodeStorage.ModelSubmit<?> submit, RenderType renderType, VertexConsumer rawConsumer,
										 OutlineBufferSource outline, MultiBufferSource.BufferSource buffers) {
		if (ZakoOptConfig.entityAnimFreeze() && submit.state() instanceof EntityRenderState state) {
			Entity entity = ((StateEntity) state).zakoopt$entity();
			if (entity != null && AnimFreeze.render(entity, model, renderType, submit.sprite(), poseStack.last().pose(), rawConsumer,
					target -> original.call(model, poseStack, target, light, overlay, color))) {
				return;
			}
		}
		original.call(model, poseStack, consumer, light, overlay, color);
	}
}
