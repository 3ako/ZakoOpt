package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererChecksMixin {
	// the Dinnerbone check builds and strips the name string per entity per frame; a custom name stays the same instance
	@Unique
	private static final Map<Component, Boolean> zakoopt$upsideDown = new IdentityHashMap<>();

	@WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;isEntityUpsideDown(Lnet/minecraft/world/entity/LivingEntity;)Z"))
	private boolean zakoopt$cachedUpsideDown(LivingEntity entity, Operation<Boolean> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(entity);
		}
		if (entity instanceof Player player) {
			// a player's name is the profile name, which never carries formatting codes
			String name = player.getGameProfile().getName();
			return ("Dinnerbone".equals(name) || "Grumm".equals(name)) && player.isModelPartShown(PlayerModelPart.CAPE);
		}
		Component name = entity.getCustomName();
		if (name == null) {
			return original.call(entity);
		}
		Boolean upsideDown = zakoopt$upsideDown.get(name);
		if (upsideDown == null) {
			// ponytail: wholesale clear instead of LRU, the visible names refill it within a frame
			if (zakoopt$upsideDown.size() >= 2048) {
				zakoopt$upsideDown.clear();
			}
			upsideDown = original.call(entity);
			zakoopt$upsideDown.put(name, upsideDown);
		}
		return upsideDown;
	}

	// isInvisibleTo answers isInvisible() or false, so a visible entity needs no team lookup
	@WrapOperation(method = "shouldShowName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"))
	private boolean zakoopt$visibleSkipsTeam(LivingEntity entity, Player player, Operation<Boolean> original) {
		return (entity.isInvisible() || !ZakoOptConfig.microOpts3()) && original.call(entity, player);
	}
}
