package zako.opt.gl;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalInt;

// the GPU side of the render scale, which changes with nearly every version
@UtilityClass
public class ScaleGpu {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/render_scale"))
			.withVertexShader(Identifier.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withFragmentShader(Identifier.fromNamespaceAndPath("zakoopt", "core/render_scale"))
			.withUniform("Globals", UniformType.UNIFORM_BUFFER)
			.withSampler("InSampler")
			.withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.WRITE_ALL))
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
			.build();

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt render scale", w, h, true);
	}

	public void fitAlongside(Minecraft mc, int w, int h) {
	}

	// the low frame stretched over the main target's colour, sharpened
	public void upscale(TextureTarget low, RenderTarget main) {
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(UPSCALE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", low.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(0, 3);
		}
	}
}
