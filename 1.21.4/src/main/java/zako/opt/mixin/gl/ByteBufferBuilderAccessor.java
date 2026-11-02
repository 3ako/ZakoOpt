package zako.opt.mixin.gl;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ByteBufferBuilder.class)
public interface ByteBufferBuilderAccessor {
	@Accessor("pointer")
	long zakoopt$pointer();
}
