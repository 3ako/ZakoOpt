package zako.opt.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlFence;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RealFence {
	public boolean creating;

	public GpuFence create() {
		return new GlFence();
	}
}
