package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.SkinAtlas;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererSkinMixin {
	// player bodies: entityTranslucent(skin) per player becomes entityTranslucent(atlas) + a sprite, one batch for all players
	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
	private void zakoopt$skinAtlas(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose, RenderType type, int light, int overlay, int color,
								   TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
		if (sprite == null && state instanceof AvatarRenderState avatar && ZakoOptConfig.skinAtlas()) {
			Identifier skin = avatar.skin.body().texturePath();
			// only the normal visible body; invisible / glow-only variants use other render types
			if (type == RenderTypes.entityTranslucent(skin)) {
				TextureAtlasSprite atlased = SkinAtlas.sprite(skin);
				if (atlased != null) {
					original.call(collector, model, state, pose, RenderTypes.entityTranslucent(SkinAtlas.ID), light, overlay, color, atlased, outline, crumbling);
					return;
				}
			}
		}
		original.call(collector, model, state, pose, type, light, overlay, color, sprite, outline, crumbling);
	}
}
