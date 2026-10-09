package zako.opt.entity;

import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AtlasTextures {
	public GpuTexture createRgba(GpuDevice device, String label, int usage, int width, int height) {
		return device.createTexture(() -> label, usage, TextureFormat.RGBA8, width, height, 1, 1);
	}

	public void clear(GpuDevice device, GpuTexture texture) {
		device.createCommandEncoder().clearColorTexture(texture, 0);
	}
}
