package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.entity.StateOwner;

// drawing looked the renderer up by type once more per entity; the renderer that extracted the state this frame is that one
@Mixin(EntityRenderDispatcher.class)
public class EntityRendererByStateMixin {
	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
			at = @At("HEAD"), cancellable = true, require = 0)
	private void zakoopt$owner(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
		if (Features.LOOKUP_CACHES.on && !(state instanceof AvatarRenderState) && ((StateOwner) state).zakoopt$owner() instanceof EntityRenderer<?, ?> owner) {
			cir.setReturnValue(owner);
		}
	}
}
