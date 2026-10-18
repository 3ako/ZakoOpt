package zako.opt.hud;

import zako.opt.feature.Features;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

// HUD drawn into its own texture at the monitor refresh rate; frames in between only blend that texture in.
// Crosshair and camera overlays (vignette, pumpkin...) blend with the world, so they stay live every frame.
@UtilityClass
public class HudCache {
	private TextureTarget target;
	// the game's own main target, taken before the HUD: mods (Xaero's minimap) swap their own one in while they draw
	private RenderTarget main;
	private boolean active;
	private boolean refresh;
	private boolean drawing;
	private boolean extracted;
	private long lastRefresh;
	public boolean suppressLiveParts;

	// once per frame, before any GUI extraction
	public void beginFrame(boolean renderLevel) {
		beginFrame(renderLevel, true);
	}

	public void beginFrame(boolean renderLevel, boolean resourcesLoaded) {
		if (!resourcesLoaded) {
			reset();
			HudDelta.idle();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean was = active;
		long now = System.nanoTime();
		active = HudWorth.update(now, MonitorRate.intervalNs(now)) && Features.HUD_CACHE.on && !HudWorth.isMainSwapped() && renderLevel && mc.level != null && HudGpu.noScreen(mc);
		if (!active) {
			HudDelta.idle();
			return;
		}
		main = HudGpu.mainTarget(mc);
		int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
		refresh = !was || target == null || target.width != w || target.height != h || now - lastRefresh >= MonitorRate.intervalNs(now);
		HudDelta.frame(refresh, mc.getDeltaTracker().getGameTimeDeltaTicks());
		if (refresh) {
			lastRefresh = now;
			if (target == null) {
				target = HudGpu.createTarget(w, h);
			} else if (target.width != w || target.height != h) {
				target.resize(w, h);
			}
		}
	}

	public void reset() {
		if (target != null) {
			target.destroyBuffers();
			target = null;
		}
		active = false;
		refresh = true;
		drawing = false;
		extracted = false;
		lastRefresh = 0;
	}

	public boolean active() {
		return active;
	}

	public boolean skipExtraction() {
		return active && !refresh;
	}

	public RenderTarget drawTarget(RenderTarget requested) {
		if (!drawing || target == null) {
			return null;
		}
		if (requested == main) {
			return target;
		}
		// Xaero's minimap draws into its own small target; a window-sized one is the main target swapped by another mod
		if (requested != target && requested.width == main.width && requested.height == main.height) {
			HudWorth.mainSwapped(requested);
		}
		return null;
	}

	// picture-in-picture renderers (Xaero's minimap) render into their own texture and may read the real frame behind them
	public boolean pipPrepare;

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && !pipPrepare && target != null && requested != target && requested == main ? target : null;
	}

	// Xaero's in-world waypoints are drawn straight to the main target from Gui.render, not through the GUI render state
	public void extractIntoTexture(Runnable extract) {
		if (target == null) {
			return;
		}
		HudGpu.clear(target);
		extracted = true;
		drawing = true;
		try {
			extract.run();
		} finally {
			drawing = false;
		}
	}

	public void drawIntoTexture(Runnable draw) {
		if (target == null) {
			return;
		}
		if (!extracted) {
			HudGpu.clear(target);
		}
		extracted = false;
		drawing = true;
		try {
			draw.run();
		} finally {
			drawing = false;
		}
	}

	public void composite() {
		if (target == null) {
			return;
		}
		RenderTarget main = HudGpu.mainTarget(Minecraft.getInstance());
		if (main != null) {
			HudGpu.composite(target, main);
		}
	}
}
