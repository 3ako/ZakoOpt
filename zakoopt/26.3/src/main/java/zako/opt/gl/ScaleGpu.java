package zako.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import zako.opt.mixin.gl.GameRendererScaleAccessor;

import java.util.Optional;
import java.util.OptionalDouble;

// the GPU side of the render scale, which changes with nearly every version
@UtilityClass
public class ScaleGpu {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/render_scale"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath("zakoopt", "core/render_scale_vk"))
			.withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
			.withColorTargetState(ColorTargetState.DEFAULT)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.build();

	public TextureTarget createTarget(int w, int h) {
		return new TextureTarget("zakoopt render scale", w, h, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
	}

	public void fitAlongside(Minecraft mc, int w, int h) {
		// the hand is drawn with this depth target next to the main one
		RenderScale.fit(((GameRendererScaleAccessor) mc.gameRenderer).zakoopt$hud3DTarget(), w, h);
	}

	// the low frame stretched over the main target's colour, sharpened
	public void upscale(TextureTarget low, RenderTarget main) {
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(UPSCALE));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("InSampler", low.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}
}
