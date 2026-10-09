package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.entity.StateEntity;

@Mixin(EntityRenderer.class)
public class EntityRendererStateMixin {
	@ModifyReturnValue(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
	private EntityRenderState zakoopt$rememberEntity(EntityRenderState state, @Local(argsOnly = true) Entity entity) {
		((StateEntity) state).zakoopt$entity(entity);
		return state;
	}
}
