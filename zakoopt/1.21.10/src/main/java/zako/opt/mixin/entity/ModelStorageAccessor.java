package zako.opt.mixin.entity;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelFeatureRenderer.Storage.class)
public interface ModelStorageAccessor {
	@Accessor("opaqueModelSubmits")
	Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> zakoopt$opaque();

	@Accessor("translucentModelSubmits")
	List<SubmitNodeStorage.TranslucentModelSubmit<?>> zakoopt$translucent();
}
