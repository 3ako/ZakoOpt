package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

// the persistent mapping of a buffer is exactly what DynamicUniformStorage would map on every write
@Mixin(GlBuffer.class)
public interface GlBufferAccessor {
	@Accessor("persistentBuffer")
	ByteBuffer zakoopt$persistentBuffer();

	@Accessor("handle")
	int zakoopt$handle();
}
