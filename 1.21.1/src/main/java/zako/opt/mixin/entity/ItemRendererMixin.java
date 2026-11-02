package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import zako.opt.entity.ItemLod;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	// set at the head of every render (1.21.1's way into renderModelLists and renderQuadList), so a stale value is never read
	@Unique
	private static boolean zakoopt$flat;

	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
	private BakedModel zakoopt$lod(BakedModel model, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack) {
		if (context.firstPerson() || context == ItemDisplayContext.GUI || !ItemLod.enabled()) {
			zakoopt$flat = false;
			return model;
		}
		Matrix4f m = poseStack.last().pose();
		zakoopt$flat = m.m30() * m.m30() + m.m31() * m.m31() + m.m32() * m.m32() > ItemLod.distanceSq();
		return model;
	}

	@ModifyVariable(method = "renderQuadList", at = @At("HEAD"), argsOnly = true)
	private List<BakedQuad> zakoopt$flatten(List<BakedQuad> quads) {
		return zakoopt$flat ? ItemLod.flat(quads) : quads;
	}
}
