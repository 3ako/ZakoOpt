package remodik.opt.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import remodik.opt.feature.Features;
import remodik.opt.hud.HudGpu;
import remodik.opt.mixin.gl.LevelRendererScaleAccessor;
import remodik.opt.mixin.gl.RenderTargetAccessor;

// the world is drawn at a lower resolution: while it renders, the main target holds the textures of a smaller one, so every
// renderer and mod that keeps the main target object draws into them. One pass then stretches it into the real main textures
// and sharpens it with AMD FSR 1 RCAS. The GUI is drawn afterwards at full resolution
@Slf4j(topic = "zakoopt")
@UtilityClass
public class RenderScale {
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
		boolean on = Features.RENDER_SCALE.on;
		// checked before anything is resized: with nothing to stretch the low frame back, the world would stay black
		if (on && !ScaleGpu.ready()) {
			Features.RENDER_SCALE.broken = true;
			log.error("Render scale shader did not compile; turned off until restart");
			on = false;
		}
		RenderTarget main = HudGpu.mainTarget(mc);
		int w = main.width, h = main.height;
		if (on) {
			w = Math.max(1, w * Features.RENDER_SCALE.percent / 100);
			h = Math.max(1, h * Features.RENDER_SCALE.percent / 100);
		}
		// the entity outlines are drawn alongside the main target, so they take its size
		RenderTarget outline = ((LevelRendererScaleAccessor) mc.levelRenderer).zakoopt$entityOutlineTarget();
		if (outline != null) {
			fit(outline, w, h);
		}
		ScaleGpu.fitAlongside(mc, w, h);
		if (!on) {
			return;
		}
		if (low == null) {
			low = ScaleGpu.createTarget(w, h);
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
		RenderTarget main = HudGpu.mainTarget(mc);
		swap(main, low);
		RenderSystem.setGlobalSettingsUniform(fullGlobals);
		ScaleGpu.upscale(low, main);
	}

	public void fit(RenderTarget target, int w, int h) {
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
