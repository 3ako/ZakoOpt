package zako.opt.mixin.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.entity.StateOwner;

@Mixin(EntityRenderer.class)
public class EntityRendererStateOwnerMixin {
	@Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
	private void zakoopt$owner(CallbackInfoReturnable<EntityRenderState> cir) {
		((StateOwner) cir.getReturnValue()).zakoopt$owner(this);
	}
}
