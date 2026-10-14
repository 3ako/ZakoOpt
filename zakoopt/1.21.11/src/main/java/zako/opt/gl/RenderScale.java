package zako.opt.gl;

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
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.resources.Identifier;
import zako.opt.feature.Features;
import zako.opt.mixin.gl.LevelRendererScaleAccessor;
import zako.opt.mixin.gl.RenderTargetAccessor;

import java.util.OptionalInt;

// the world is drawn at a lower resolution: while it renders, the main target holds the textures of a smaller one, so every
// renderer and mod that keeps the main target object draws into them. One pass then stretches it into the real main textures
// and sharpens it with AMD FSR 1 RCAS. The GUI is drawn afterwards at full resolution
@UtilityClass
public class RenderScale {
	private final RenderPipeline UPSCALE = RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("zakoopt", "pipeline/fsr_upscale"))
			.withVertexShader("core/screenquad")
			.withFragmentShader(Identifier.fromNamespaceAndPath("zakoopt", "core/fsr_upscale"))
			.withUniform("Globals", UniformType.UNIFORM_BUFFER)
			.withSampler("InSampler")
			.withDepthWrite(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
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
		RenderTarget main = mc.getMainRenderTarget();
		int w = main.width, h = main.height;
		if (Features.RENDER_SCALE.on) {
			w = Math.max(1, w * Features.RENDER_SCALE.percent / 100);
			h = Math.max(1, h * Features.RENDER_SCALE.percent / 100);
		}
		// the entity outlines are drawn alongside the main target, so they take its size
		RenderTarget outline = ((LevelRendererScaleAccessor) mc.levelRenderer).zakoopt$entityOutlineTarget();
		if (outline != null) {
			fit(outline, w, h);
		}
		if (!Features.RENDER_SCALE.on) {
			return;
		}
		if (low == null) {
			low = new TextureTarget("zakoopt render scale", w, h, true);
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
		RenderTarget main = mc.getMainRenderTarget();
		swap(main, low);
		RenderSystem.setGlobalSettingsUniform(fullGlobals);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "zakoopt render scale", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(UPSCALE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.bindTexture("InSampler", low.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.draw(0, 3);
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
