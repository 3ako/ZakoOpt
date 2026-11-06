package zako.opt.mixin.gl;

import zako.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.caffeinemc.mods.sodium.client.render.vertex.serializers.VertexSerializerRegistryImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// every buffer copy between two formats looked its serializer up under a lock; nearly all of them are the same pair
@Mixin(value = VertexSerializerRegistryImpl.class, remap = false)
public class SodiumSerializerCacheMixin {
	@Unique
	private record Last(long key, VertexSerializer serializer) {
	}

	@Unique
	private volatile Last zakoopt$last;

	@WrapOperation(method = "get", require = 0,
			at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/vertex/serializers/VertexSerializerRegistryImpl;find(J)Lnet/caffeinemc/mods/sodium/api/vertex/serializer/VertexSerializer;"))
	private VertexSerializer zakoopt$cached(VertexSerializerRegistryImpl registry, long key, Operation<VertexSerializer> original) {
		if (!Features.LOOKUP_CACHES.on) {
			return original.call(registry, key);
		}
		Last last = zakoopt$last;
		if (last != null && last.key() == key) {
			return last.serializer();
		}
		VertexSerializer serializer = original.call(registry, key);
		if (serializer != null) {
			zakoopt$last = new Last(key, serializer);
		}
		return serializer;
	}
}
