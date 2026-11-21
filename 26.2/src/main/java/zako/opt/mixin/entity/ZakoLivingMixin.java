package zako.opt.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.cosmetic.*;
import zako.opt.feature.Features;
@Mixin(LivingEntityRenderer.class)
public abstract class ZakoLivingMixin {
 @Shadow protected EntityModel<?> model;
 @Unique private static PlayerModel zakoopt$model;
 @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
 private void zakoopt$character(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
  var player = Minecraft.getInstance().player;
  if (!((Object)this instanceof AvatarRenderer<?>) || !(state instanceof AvatarRenderState avatar) || player == null || avatar.id != player.getId() || !Features.ZAKO_COSMETIC.on || !BlenderCharacterMesh.available()) { original.call(state,pose,collector,camera); return; }
  if (zakoopt$model == null) zakoopt$model = ZakoCosmeticModel.create();
  EntityModel<?> previous = model; model = zakoopt$model;
  try { original.call(state,pose,collector,camera); } finally { model = previous; }
 }
 @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value="INVOKE", target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
 private void zakoopt$mesh(SubmitNodeCollector collector, Model<?> drawn, Object state, PoseStack pose, RenderType type, int light, int overlay, int color, TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
  var player = Minecraft.getInstance().player;
  if (drawn == zakoopt$model && drawn instanceof PlayerModel pm && state instanceof AvatarRenderState avatar && player != null && avatar.id == player.getId() && Features.ZAKO_COSMETIC.on && BlenderCharacterMesh.available()) BlenderCharacterMesh.submit(pm, avatar, pose, collector, type, light, overlay, color);
  else original.call(collector,drawn,state,pose,type,light,overlay,color,sprite,outline,crumbling);
 }
}
