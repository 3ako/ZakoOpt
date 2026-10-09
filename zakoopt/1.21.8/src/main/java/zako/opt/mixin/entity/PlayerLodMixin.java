package zako.opt.mixin.entity;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.EntityLod;

@Mixin(PlayerRenderer.class)
public class 	PlayerLodMixin {
	// far players lose the outer skin layer: six more cuboids per player, close to half the model
	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
	private void zakoopt$farSkinLayer(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
		if (EntityLod.farPlayer(state.distanceToCameraSq)) {
			state.showHat = false;
			state.showJacket = false;
			state.showLeftPants = false;
			state.showRightPants = false;
			state.showLeftSleeve = false;
			state.showRightSleeve = false;
			state.showCape = false;
		}
	}
}
