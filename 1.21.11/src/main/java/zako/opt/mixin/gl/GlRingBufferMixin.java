package zako.opt.mixin.gl;

import net.minecraft.client.renderer.MappableRingBuffer;
import zako.opt.feature.Features;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// four slots are always allocated, so the switch can be flipped without a restart; only the rotation changes. With three
// slots a GPU still reading a buffer forces currentBuffer() to wait for it before the CPU can write the next frame
@Mixin(MappableRingBuffer.class)
public class GlRingBufferMixin {
	@ModifyConstant(method = "<init>", constant = @Constant(intValue = 3))
	private static int zakoopt$slots(int three) {
		return 4;
	}

	@ModifyConstant(method = "rotate", constant = @Constant(intValue = 3))
	private int zakoopt$rotation(int three) {
		return Features.RING_BUFFERS.on ? 4 : 3;
	}

	@ModifyConstant(method = "close", constant = @Constant(intValue = 3))
	private int zakoopt$close(int three) {
		return 4;
	}
}
