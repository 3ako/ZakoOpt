package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.entity.CachedRenderer;

import java.util.Map;

// the renderer lookup runs a type switch and a map get for every entity several times a frame; the answer is kept on the
// entity, tied to the renderer map so a resource reload drops it. Only renderers looked up by type are kept: a player's
// depends on the skin model, which can change once the skin loads
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererLookupMixin {
	@Shadow
	private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

	@Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
	@SuppressWarnings("unchecked")
	private void zakoopt$cached(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (Features.LOOKUP_CACHES.on && ((CachedRenderer) entity).zakoopt$rendererMap() == renderers) {
			cir.setReturnValue((EntityRenderer<?, ?>) ((CachedRenderer) entity).zakoopt$renderer());
		}
	}

	@Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("RETURN"))
	private void zakoopt$keep(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (Features.LOOKUP_CACHES.on && renderers.get(entity.getType()) == cir.getReturnValue()) {
			((CachedRenderer) entity).zakoopt$setRenderer(cir.getReturnValue(), renderers);
		}
	}
}
