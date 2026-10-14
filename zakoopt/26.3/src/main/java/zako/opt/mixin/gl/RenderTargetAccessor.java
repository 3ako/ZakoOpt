package zako.opt.mixin.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTarget.class)
public interface RenderTargetAccessor {
	@Accessor("colorTexture")
	GpuTexture zakoopt$colorTexture();

	@Accessor("colorTexture")
	void zakoopt$colorTexture(GpuTexture texture);

	@Accessor("colorTextureView")
	GpuTextureView zakoopt$colorTextureView();

	@Accessor("colorTextureView")
	void zakoopt$colorTextureView(GpuTextureView view);

	@Accessor("depthTexture")
	GpuTexture zakoopt$depthTexture();

	@Accessor("depthTexture")
	void zakoopt$depthTexture(GpuTexture texture);

	@Accessor("depthTextureView")
	GpuTextureView zakoopt$depthTextureView();

	@Accessor("depthTextureView")
	void zakoopt$depthTextureView(GpuTextureView view);
}
