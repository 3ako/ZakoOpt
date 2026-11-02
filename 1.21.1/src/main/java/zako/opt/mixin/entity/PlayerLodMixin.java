package zako.opt.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.EntityLod;

@Mixin(PlayerRenderer.class)
public abstract class PlayerLodMixin {
	// far players lose the outer skin layer: six more cuboids per player, close to half the model (the cape is a layer, gone already)
	@Inject(method = "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V", at = @At("TAIL"))
	private void zakoopt$farSkinLayer(AbstractClientPlayer player, CallbackInfo ci) {
		if (EntityLod.farPlayer(Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(player))) {
			PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();
			model.hat.visible = false;
			model.jacket.visible = false;
			model.leftPants.visible = false;
			model.rightPants.visible = false;
			model.leftSleeve.visible = false;
			model.rightSleeve.visible = false;
		}
	}
}
