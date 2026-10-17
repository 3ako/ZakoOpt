package remodik.opt.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalInt;

// the GPU side of the render scale, which changes with nearly every version
@UtilityClass
public class ScaleGpu {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withLocation(ResourceLocation.fromNamespaceAndPath("zakoopt", "pipeline/render_scale"))
			.withVertexShader(ResourceLocation.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withFragmentShader(ResourceLocation.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withUniform("Globals", UniformType.UNIFORM_BUFFER)
			.withSampler("InSampler")
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build();
	// 1.21.8 draws only with a vertex buffer bound, though the shader reads none of it
	private GpuBuffer vertices;

	// the game skips draws with a pipeline that failed to compile instead of throwing
	public boolean ready() {
		return RenderSystem.getDevice().precompilePipeline(UPSCALE).isValid();
	}

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt render scale", w, h, true);
	}

	public void fitAlongside(Minecraft mc, int w, int h) {
	}

	// the low frame stretched over the main target's colour, sharpened
	public void upscale(TextureTarget low, RenderTarget main) {
		// the sampler comes with the texture
		low.getColorTexture().setTextureFilter(FilterMode.LINEAR, false);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), OptionalInt.empty())) {
			if (vertices == null) {
				vertices = RenderSystem.getDevice().createBuffer(() -> "zakoopt render scale", GpuBuffer.USAGE_VERTEX, 16);
			}
			pass.setPipeline(UPSCALE);
			pass.setVertexBuffer(0, vertices);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindSampler("InSampler", low.getColorTextureView());
			pass.draw(0, 3);
		}
	}
}
