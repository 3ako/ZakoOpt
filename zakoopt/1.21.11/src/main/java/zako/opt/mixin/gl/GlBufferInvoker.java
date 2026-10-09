package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlBuffer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@Mixin(GlBuffer.class)
public interface GlBufferInvoker {
	@Invoker("<init>")
	static GlBuffer zakoopt$create(@Nullable Supplier<String> label, DirectStateAccess dsa, int usage, long size, int handle, @Nullable ByteBuffer persistent) {
		throw new AssertionError();
	}
}
