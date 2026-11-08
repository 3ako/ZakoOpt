package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.SkinAtlas;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererSkinMixin {
	// player bodies: entityTranslucent(skin) per player becomes entityTranslucent(atlas) with UVs remapped into the skin's slot
	@WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	private VertexConsumer zakoopt$skinAtlas(MultiBufferSource buffers, RenderType type, Operation<VertexConsumer> original, LivingEntity entity) {
		// glowing bodies stay on their own skin: one shared outline type for all players never gets flushed by a type switch.
		// Only into the world's buffers: a mod recording the model into its own (Real Camera finds the head by the skin's type, #17) sees the skin
		if (entity instanceof AbstractClientPlayer player && Features.SKIN_ATLAS.on && buffers == Minecraft.getInstance().renderBuffers().bufferSource()) {
			ResourceLocation skin = player.getSkin().texture();
			// only the normal visible body; invisible / glow-only variants use other render types
			if (type == RenderType.entityTranslucent(skin)) {
				TextureAtlasSprite atlased = SkinAtlas.sprite(skin);
				if (atlased != null) {
					return atlased.wrap(original.call(buffers, RenderType.entityTranslucent(SkinAtlas.ID)));
				}
			}
		}
		return original.call(buffers, type);
	}
}
