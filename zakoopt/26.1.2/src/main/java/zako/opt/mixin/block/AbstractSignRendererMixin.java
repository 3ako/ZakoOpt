package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.util.IdentityHashMap;
import java.util.Map;

// every sign, every frame looked its sprite up by WoodType, a record whose hashCode walks all fields; the answer never changes per renderer
@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
	@Unique
	private final Map<WoodType, SpriteId> zakoopt$sprites = new IdentityHashMap<>();

	@WrapOperation(method = "submitSign", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;getSignSprite(Lnet/minecraft/world/level/block/state/properties/WoodType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"))
	private SpriteId zakoopt$sprite(AbstractSignRenderer<?> renderer, WoodType wood, Operation<SpriteId> original) {
		if (!ZakoOptConfig.signCache()) {
			return original.call(renderer, wood);
		}
		return zakoopt$sprites.computeIfAbsent(wood, w -> original.call(renderer, w));
	}
}
