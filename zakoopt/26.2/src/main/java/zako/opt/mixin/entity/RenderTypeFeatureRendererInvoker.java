package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererInvoker {
	@Invoker("getVertexBuilder")
	VertexConsumer zakoopt$vertexBuilder(RenderType renderType);
}
