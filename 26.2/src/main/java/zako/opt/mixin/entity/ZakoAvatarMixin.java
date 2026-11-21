package zako.opt.mixin.entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import zako.opt.cosmetic.*;
import zako.opt.feature.Features;
@Mixin(AvatarRenderer.class)
public class ZakoAvatarMixin {
 @Unique private static PlayerModel zakoopt$hand;
 @Unique private static boolean zakoopt$enabled() { return Features.ZAKO_COSMETIC.on && BlenderCharacterMesh.available(); }
 @Unique private static Identifier zakoopt$texture() {
  var f = Features.ZAKO_COSMETIC;
  if (f.logo >= 4 && f.logo <= 6) return zako.opt.cosmetic.ZakoLogoTextures.get(f.logo, f.white);
  String logo = switch (f.logo) { case 1 -> "_kfc"; case 2 -> "_mcdonalds"; case 3 -> "_holyworld"; default -> ""; };
  return Identifier.fromNamespaceAndPath("zakoopt", "textures/cosmetic/zako" + logo + (f.white ? "_white" : "") + ".png");
 }
 @Inject(method="getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at=@At("HEAD"), cancellable=true)
 private void zakoopt$texture(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) { var p = Minecraft.getInstance().player; if (p != null && state.id == p.getId() && zakoopt$enabled()) cir.setReturnValue(zakoopt$texture()); }
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at=@At("TAIL"))
 private void zakoopt$layers(Avatar avatar, AvatarRenderState state, float tick, CallbackInfo ci) {
  if (avatar == Minecraft.getInstance().player && zakoopt$enabled()) {
   state.showCape = false;
   if (state.headEquipment.is(net.minecraft.tags.ItemTags.HEAD_ARMOR)) state.headEquipment = net.minecraft.world.item.ItemStack.EMPTY;
   if (state.chestEquipment.is(net.minecraft.tags.ItemTags.CHEST_ARMOR)) state.chestEquipment = net.minecraft.world.item.ItemStack.EMPTY;
   if (state.legsEquipment.is(net.minecraft.tags.ItemTags.LEG_ARMOR)) state.legsEquipment = net.minecraft.world.item.ItemStack.EMPTY;
   if (state.feetEquipment.is(net.minecraft.tags.ItemTags.FOOT_ARMOR)) state.feetEquipment = net.minecraft.world.item.ItemStack.EMPTY;
  }
 }
 @Inject(method="renderRightHand", at=@At("HEAD"), cancellable=true)
 private void zakoopt$right(PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture, boolean sleeve, CallbackInfo ci) { if (zakoopt$hand(pose,collector,light,true)) ci.cancel(); }
 @Inject(method="renderLeftHand", at=@At("HEAD"), cancellable=true)
 private void zakoopt$left(PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture, boolean sleeve, CallbackInfo ci) { if (zakoopt$hand(pose,collector,light,false)) ci.cancel(); }
 @Unique private boolean zakoopt$hand(PoseStack pose, SubmitNodeCollector collector, int light, boolean right) {
  if (Minecraft.getInstance().player == null || !zakoopt$enabled()) return false;
  if (zakoopt$hand == null) zakoopt$hand = ZakoCosmeticModel.create();
  var arm = right ? zakoopt$hand.rightArm : zakoopt$hand.leftArm; arm.resetPose(); arm.visible = true; arm.zRot = right ? .1f : -.1f;
  BlenderCharacterMesh.submitArm(right, arm, pose, collector, RenderTypes.entityTranslucent(zakoopt$texture()), light, OverlayTexture.NO_OVERLAY); return true;
 }
}
