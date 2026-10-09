package zako.opt.mixin.entity;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.entity.EntityLod;

@Mixin(AvatarRenderer.class)
public class AvatarLodMixin {
	// far players lose the outer skin layer: six more cuboids per player, close to half the model
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void zakoopt$farSkinLayer(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
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
