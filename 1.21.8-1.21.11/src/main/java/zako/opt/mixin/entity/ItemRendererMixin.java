package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import zako.opt.entity.ItemLod;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
	private static List<BakedQuad> zakoopt$lod(List<BakedQuad> quads, ItemDisplayContext context, PoseStack poseStack) {
		if (!ItemLod.applies(context)) {
			return quads;
		}
		return ItemLod.far(poseStack.last().pose()) ? ItemLod.flat(quads) : quads;
	}
}
