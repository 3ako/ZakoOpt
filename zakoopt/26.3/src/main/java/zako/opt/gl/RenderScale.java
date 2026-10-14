package zako.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.resources.Identifier;
import zako.opt.feature.Features;
import zako.opt.mixin.gl.GameRendererScaleAccessor;
import zako.opt.mixin.gl.LevelRendererScaleAccessor;
import zako.opt.mixin.gl.RenderTargetAccessor;

import java.util.Optional;
import java.util.OptionalDouble;

// the world is drawn at a lower resolution: while it renders, the main target holds the textures of a smaller one, so every
// renderer and mod that keeps the main target object draws into them. One pass then stretches it into the real main textures
// and sharpens it with AMD FSR 1 RCAS. The GUI is drawn afterwards at full resolution
@UtilityClass
public class RenderScale {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/fsr_upscale"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath("zakoopt", "core/fsr_upscale"))
			.withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
			.withColorTargetState(ColorTargetState.DEFAULT)
			.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
			.build();

	private TextureTarget low;
	private GlobalSettingsUniform lowGlobals;
	private GpuBuffer fullGlobals;
	// this frame's global settings, to be sent again with another screen size
	private Globals globals;
	private boolean scaled;

	public interface Globals {
		void update(GlobalSettingsUniform uniform, int width, int height);
	}

	public void globals(Globals update) {
		globals = update;
	}

	public void begin(Minecraft mc) {
		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		int w = main.width, h = main.height;
		if (Features.RENDER_SCALE.on) {
			w = Math.max(1, w * Features.RENDER_SCALE.percent / 100);
			h = Math.max(1, h * Features.RENDER_SCALE.percent / 100);
		}
		// the hand's depth and the entity outlines are drawn alongside the main target, so they take its size
		fit(((GameRendererScaleAccessor) mc.gameRenderer).zakoopt$hud3DTarget(), w, h);
		fit(((LevelRendererScaleAccessor) mc.levelRenderer).zakoopt$entityOutlineTarget(), w, h);
		if (!Features.RENDER_SCALE.on) {
			return;
		}
		if (low == null) {
			low = new TextureTarget("zakoopt render scale", w, h, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
			lowGlobals = new GlobalSettingsUniform();
		} else {
			fit(low, w, h);
		}
		fullGlobals = RenderSystem.getGlobalSettingsUniform();
		if (globals != null) {
			globals.update(lowGlobals, w, h);
		}
		swap(main, low);
		scaled = true;
	}

	public void end(Minecraft mc) {
		if (!scaled) {
			return;
		}
		scaled = false;
		RenderTarget main = mc.gameRenderer.mainRenderTarget();
		swap(main, low);
		RenderSystem.setGlobalSettingsUniform(fullGlobals);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(UPSCALE));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("InSampler", low.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(3, 1, 0, 0);
		}
	}

	private void fit(RenderTarget target, int w, int h) {
		if (target.width != w || target.height != h) {
			target.resize(w, h);
		}
	}

	private void swap(RenderTarget a, RenderTarget b) {
		RenderTargetAccessor x = (RenderTargetAccessor) a, y = (RenderTargetAccessor) b;
		int w = a.width, h = a.height;
		a.width = b.width;
		a.height = b.height;
		b.width = w;
		b.height = h;
		var color = x.zakoopt$colorTexture();
		x.zakoopt$colorTexture(y.zakoopt$colorTexture());
		y.zakoopt$colorTexture(color);
		var colorView = x.zakoopt$colorTextureView();
		x.zakoopt$colorTextureView(y.zakoopt$colorTextureView());
		y.zakoopt$colorTextureView(colorView);
		var depth = x.zakoopt$depthTexture();
		x.zakoopt$depthTexture(y.zakoopt$depthTexture());
		y.zakoopt$depthTexture(depth);
		var depthView = x.zakoopt$depthTextureView();
		x.zakoopt$depthTextureView(y.zakoopt$depthTextureView());
		y.zakoopt$depthTextureView(depthView);
	}
}
