package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import zako.opt.entity.ItemLod;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
	private static List<BakedQuad> zakoopt$lod(List<BakedQuad> quads, ItemDisplayContext context, PoseStack poseStack) {
		if (context.firstPerson() || context == ItemDisplayContext.GUI || !ItemLod.enabled()) {
			return quads;
		}
		Matrix4f m = poseStack.last().pose();
		float dSq = m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32();
		return dSq > ItemLod.distanceSq() ? ItemLod.flat(quads) : quads;
	}
}
