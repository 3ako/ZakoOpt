package zako.opt.entity;

import com.mojang.blaze3d.GpuFormat;
import org.joml.Vector4f;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AtlasTextures {
	public GpuTexture createRgba(GpuDevice device, String label, int usage, int width, int height) {
		return device.createTexture(() -> label, usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
	}

	public void clear(GpuDevice device, GpuTexture texture) {
		device.createCommandEncoder().clearColorTexture(texture, new Vector4f(0, 0, 0, 0));
	}
}
