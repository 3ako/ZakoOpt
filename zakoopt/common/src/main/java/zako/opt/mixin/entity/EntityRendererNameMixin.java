package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.CachedName;

@Mixin(EntityRenderer.class)
public class EntityRendererNameMixin {
	// a player's name is rebuilt (team prefix + name + suffix) every frame; the same instance keeps the text caches hitting by identity
	@WrapOperation(method = "getNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getDisplayName()Lnet/minecraft/network/chat/Component;"))
	private Component zakoopt$cachedName(Entity entity, Operation<Component> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(entity);
		}
		CachedName cache = (CachedName) entity;
		long tick = entity.level().getGameTime();
		if (cache.zakoopt$nameTick() != tick) {
			cache.zakoopt$setName(original.call(entity), tick);
		}
		return cache.zakoopt$name();
	}
}
