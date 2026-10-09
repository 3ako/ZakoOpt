package zako.opt.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RealFence {
	// set while asking the encoder for a real fence, so the createFence mixin lets that one through
	public boolean creating;

	public GpuFence create() {
		creating = true;
		try {
			return RenderSystem.getDevice().createCommandEncoder().createFence();
		} finally {
			creating = false;
		}
	}
}
