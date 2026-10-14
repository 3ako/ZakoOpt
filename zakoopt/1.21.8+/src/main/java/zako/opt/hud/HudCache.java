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
	private long lastRefresh;
	public boolean suppressLiveParts;

	// once per frame, before any GUI extraction
	public void beginFrame(boolean renderLevel) {
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

	public boolean active() {
		return active;
	}

	public boolean skipExtraction() {
		return active && !refresh;
	}

	public RenderTarget drawTarget(RenderTarget requested) {
		if (!drawing) {
			return null;
		}
		if (requested == main) {
			return target;
		}
		// Xaero's minimap draws into its own small target; a window-sized one is the main target swapped by another mod
		if (requested != target && requested.width == main.width && requested.height == main.height) {
			HudWorth.mainSwapped();
		}
		return null;
	}

	// picture-in-picture renderers (Xaero's minimap) render into their own texture and may read the real frame behind them
	public boolean pipPrepare;

	public RenderTarget redirect(RenderTarget requested) {
		return drawing && !pipPrepare && requested != target && requested == main ? target : null;
	}

	public void drawIntoTexture(Runnable draw) {
		HudGpu.clear(target);
		drawing = true;
		try {
			draw.run();
		} finally {
			drawing = false;
		}
		HudTiles.mark(target);
	}

	public void composite() {
		RenderTarget main = HudGpu.mainTarget(Minecraft.getInstance());
		if (!HudTiles.blend(target, main)) {
			HudGpu.composite(target, main);
		}
	}
}
