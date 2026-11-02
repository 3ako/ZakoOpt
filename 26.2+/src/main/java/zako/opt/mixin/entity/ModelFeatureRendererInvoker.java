package zako.opt.mixin.entity;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ModelFeatureRenderer.class)
public interface ModelFeatureRendererInvoker {
	@Invoker("prepareModel")
	<S> void zakoopt$prepareModel(ModelFeatureRenderer.Submit<S> submit);
}
