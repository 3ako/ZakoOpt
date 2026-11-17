package zako.opt.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RealFence {
	public GpuFence create() {
		return RenderSystem.getDevice().createCommandEncoder().createFence();
	}
}
