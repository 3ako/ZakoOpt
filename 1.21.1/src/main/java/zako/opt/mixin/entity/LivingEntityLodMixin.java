package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.EntityLod;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityLodMixin {
	// far entities skip their layers: armour, held items, eyes, cape, elytra, stuck arrows (1.21.1 gates the layers on isSpectator)
	@ModifyExpressionValue(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSpectator()Z"))
	private boolean zakoopt$farLayers(boolean spectator, LivingEntity entity) {
		if (spectator) {
			return true;
		}
		double d = Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);
		return entity instanceof Player ? EntityLod.farPlayer(d) : EntityLod.far(d);
	}
}
