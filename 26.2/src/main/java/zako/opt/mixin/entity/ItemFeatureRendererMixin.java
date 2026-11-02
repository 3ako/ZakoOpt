package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.ItemLod;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {
	@WrapOperation(method = {"prepareMainSubmit", "prepareOutlineSubmit", "prepareFoilSubmit"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;quads()Ljava/util/List;"))
	private List<BakedQuad> zakoopt$lod(ItemFeatureRenderer.Submit submit, Operation<List<BakedQuad>> original) {
		List<BakedQuad> quads = original.call(submit);
		ItemDisplayContext context = submit.displayContext();
		if (context.firstPerson() || context == ItemDisplayContext.GUI || !ItemLod.enabled()) {
			return quads;
		}
		Matrix4f m = submit.pose().pose();
		float dSq = m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32();
		return dSq > ItemLod.distanceSq() ? ItemLod.flat(quads) : quads;
	}
}
