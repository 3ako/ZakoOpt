package zako.opt.hud;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import zako.opt.ZakoOptConfig;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
	private TextureTarget target;
	private boolean active;
	private boolean refresh;
	private long lastRefresh;
	public boolean drawing;
	public boolean suppressLiveParts;

	public void beginFrame(boolean renderLevel) {
		Minecraft mc = Minecraft.getInstance();
		boolean was = active;
		long now = System.nanoTime();
		active = HudWorth.update(now, MonitorRate.intervalNs(now)) && ZakoOptConfig.hudCache() && renderLevel && mc.level != null && mc.screen == null && mc.getOverlay() == null;
		if (!active) {
			return;
		}
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		if (refresh) {
			lastRefresh = now;
			if (target == null) {
				target = new TextureTarget(w, h, true);
				target.setClearColor(0, 0, 0, 0);
			} else if (target.width != w || target.height != h) {
				target.resize(w, h);
			}
		}
	}

	public boolean active() {
		return active;
	}

	public boolean skipExtraction() {
		return active && !refresh;
	}

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && requested != target && requested == Minecraft.getInstance().getMainRenderTarget() ? target : null;
	}

	public void drawIntoTexture(Runnable draw) {
		// clear() leaves framebuffer 0 bound
		target.clear();
		target.bindWrite(true);
		drawing = true;
		// re-sends the blend func so the alpha override reaches GL past GlStateManager's cache
		RenderSystem.defaultBlendFunc();
		try {
			draw.run();
		} finally {
			drawing = false;
			RenderSystem.defaultBlendFunc();
			Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		}
	}

	// the texture holds premultiplied colour (GUI blending onto transparent black leaves colour * alpha)
	public void composite() {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		RenderSystem.enableBlend();
		GlStateManager._blendFunc(GlStateManager.SourceFactor.ONE.value, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA.value);
		target.blitAndBlendToScreen(main.width, main.height);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
	}
}
