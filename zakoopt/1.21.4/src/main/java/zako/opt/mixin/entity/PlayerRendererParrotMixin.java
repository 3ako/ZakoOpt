package zako.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Parrot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(PlayerRenderer.class)
public class PlayerRendererParrotMixin {
	// the shoulder tag is the synced data's own instance until it changes; vanilla re-reads its id and variant every frame
	@Unique
	private static final Map<CompoundTag, Object> zakoopt$parrots = new IdentityHashMap<>();
	@Unique
	private static final Object zakoopt$none = new Object();

	@WrapOperation(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;getParrotOnShoulder(Lnet/minecraft/client/player/AbstractClientPlayer;Z)Lnet/minecraft/world/entity/animal/Parrot$Variant;"))
	private Parrot.Variant zakoopt$cachedParrot(AbstractClientPlayer player, boolean left, Operation<Parrot.Variant> original) {
		if (!ZakoOptConfig.microOpts3()) {
			return original.call(player, left);
		}
		CompoundTag tag = left ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
		Object parrot = zakoopt$parrots.get(tag);
		if (parrot == null) {
			// ponytail: wholesale clear instead of LRU, two tags per visible player refill it within a frame
			if (zakoopt$parrots.size() >= 2048) {
				zakoopt$parrots.clear();
			}
			Parrot.Variant variant = original.call(player, left);
			parrot = variant == null ? zakoopt$none : variant;
			zakoopt$parrots.put(tag, parrot);
		}
		return parrot == zakoopt$none ? null : (Parrot.Variant) parrot;
	}
}
