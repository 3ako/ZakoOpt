package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ModelFeatureRenderer.class)
public interface ModelFeatureRendererInvoker {
	@Invoker("renderModel")
	<S> void zakoopt$renderModel(SubmitNodeStorage.ModelSubmit<S> submit, RenderType renderType, VertexConsumer consumer,
								  OutlineBufferSource outline, MultiBufferSource.BufferSource crumbling);
}
