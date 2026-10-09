package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.ZakoOptConfig;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils", remap = false)
public class VertexConsumerUtilsMixin {
	// last consumer that passed the check; a whole model goes to one consumer, so this hits for nearly every cuboid
	@Unique
	private static volatile Object zakoopt$lastWriter;

	// tryOf is an interface instanceof plus an interface call on a handful of alternating classes,
	// which keeps evicting HotSpot's per-class type check cache; an identity compare skips both
	@WrapMethod(method = "convertOrLog")
	private static VertexBufferWriter zakoopt$cachedConvert(VertexConsumer consumer, Operation<VertexBufferWriter> original) {
		if (consumer == zakoopt$lastWriter && ZakoOptConfig.writerCache()) {
			return (VertexBufferWriter) consumer;
		}
		VertexBufferWriter writer = original.call(consumer);
		if (writer == consumer) {
			zakoopt$lastWriter = consumer;
		}
		return writer;
	}
}
