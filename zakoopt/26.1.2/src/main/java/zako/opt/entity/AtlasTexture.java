package zako.opt.entity;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;

// an RGBA texture the size of a sprite atlas, sampled nearest with repeat, cleared to transparent; filled on the GPU
public class AtlasTexture extends AbstractTexture {
	public AtlasTexture(String label, int size) {
		GpuDevice device = RenderSystem.getDevice();
		texture = device.createTexture(() -> label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, size, size, 1, 1);
		textureView = device.createTextureView(texture);
		sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
		device.createCommandEncoder().clearColorTexture(texture, 0);
	}
}
