package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.EntityLod;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityLodMixin {
	// far entities skip their layers: armour, held items, eyes, cape, elytra, stuck arrows
	@ModifyExpressionValue(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z"))
	private boolean zakoopt$farLayers(boolean render, LivingEntityRenderState state) {
		return render && !(state instanceof PlayerRenderState ? EntityLod.farPlayer(state.distanceToCameraSq) : EntityLod.far(state.distanceToCameraSq));
	}
}
