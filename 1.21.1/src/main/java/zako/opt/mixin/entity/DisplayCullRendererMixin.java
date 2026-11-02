package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class DisplayCullRendererMixin {
	@ModifyExpressionValue(method = "shouldRender", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;noCulling:Z"))
	private boolean zakoopt$cull(boolean noCulling, Entity entity) {
		return noCulling && !(entity instanceof Display && Features.DISPLAY_CULL.on);
	}
}
