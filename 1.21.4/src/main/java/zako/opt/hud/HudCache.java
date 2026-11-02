package zako.opt.hud;

import zako.opt.feature.Features;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
	private TextureTarget target;
	// the game's own main target, taken before the HUD: Lunar overrides getMainRenderTarget() with its own one while it draws
	private RenderTarget main;
	private boolean active;
	private boolean refresh;
	private long lastRefresh;
	public boolean drawing;
	public boolean suppressLiveParts;

	public void beginFrame(boolean renderLevel) {
		Minecraft mc = Minecraft.getInstance();
		boolean was = active;
		long now = System.nanoTime();
		active = HudWorth.update(now, MonitorRate.intervalNs(now)) && Features.HUD_CACHE.on && !HudWorth.isMainSwapped() && renderLevel && mc.level != null && mc.screen == null && mc.getOverlay() == null;
		if (!active) {
			HudDelta.idle();
			return;
		}
		main = mc.getMainRenderTarget();
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		HudDelta.frame(refresh, mc.getDeltaTracker().getGameTimeDeltaTicks());
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
		if (!drawing) {
			return null;
		}
		RenderTarget current = Minecraft.getInstance().getMainRenderTarget();
		if (current != main) {
			HudWorth.mainSwapped(current);
			return null;
		}
		return requested == main ? target : null;
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
