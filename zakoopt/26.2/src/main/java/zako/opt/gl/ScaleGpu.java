package zako.opt.gl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalDouble;

// the GPU side of the render scale, which changes with nearly every version
@UtilityClass
public class ScaleGpu {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/render_scale"))
			.withVertexShader(Identifier.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withFragmentShader(Identifier.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
			.withColorTargetState(new ColorTargetState(Optional.empty(), GpuFormat.RGBA8_UNORM, 15))
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.build();

	// the game skips draws with a pipeline that failed to compile instead of throwing
	public boolean ready() {
		return RenderSystem.getDevice().precompilePipeline(UPSCALE).isValid();
	}

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt render scale", w, h, true, GpuFormat.RGBA8_UNORM);
	}

	public void fitAlongside(Minecraft mc, int w, int h) {
	}

	// the low frame stretched over the main target's colour, sharpened
	public void upscale(TextureTarget low, RenderTarget main) {
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
			pass.setPipeline(UPSCALE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", low.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}
}
