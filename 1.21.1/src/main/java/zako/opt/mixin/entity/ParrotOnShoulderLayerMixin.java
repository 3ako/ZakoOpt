package zako.opt.mixin.entity;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(ParrotOnShoulderLayer.class)
public class ParrotOnShoulderLayerMixin {
	// the shoulder tag is the synced data's own instance until it changes; vanilla parses its id into an entity type every frame
	@Unique
	private static final Map<CompoundTag, Optional<EntityType<?>>> zakoopt$types = new IdentityHashMap<>();

	@WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/player/Player;FFFFZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;byString(Ljava/lang/String;)Ljava/util/Optional;"))
	private Optional<EntityType<?>> zakoopt$cachedType(String id, Operation<Optional<EntityType<?>>> original, @Local CompoundTag tag) {
		if (!Features.MICRO_OPTS.fewerChecks) {
			return original.call(id);
		}
		Optional<EntityType<?>> type = zakoopt$types.get(tag);
		if (type == null) {
			// ponytail: wholesale clear instead of LRU, two tags per visible player refill it within a frame
			if (zakoopt$types.size() >= 2048) {
				zakoopt$types.clear();
			}
			type = original.call(id);
			zakoopt$types.put(tag, type);
		}
		return type;
	}
}
