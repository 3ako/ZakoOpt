package zako.opt.mixin.entity;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

// the visible entities are sorted by type every frame through Comparator.comparing, which boxed both hash codes on
// every comparison (a sixth of the render thread's garbage on a farm); the same order without boxing
@Mixin(LevelRenderer.class)
public class EntitySortMixin {
	@Shadow
	@Final
	@Mutable
	private static Comparator<Entity> ENTITY_COMPARATOR;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void zakoopt$intComparator(CallbackInfo ci) {
		ENTITY_COMPARATOR = Comparator.comparingInt(entity -> entity.getType().hashCode());
	}
}
