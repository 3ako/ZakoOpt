package zako.opt.hud;

import lombok.experimental.UtilityClass;

// mods like Smooth Scroll step HUD animations by the last frame's delta each time the HUD is drawn. The cached HUD is drawn
// only on refresh frames, so while it is, the delta handed out covers every frame since the previous refresh
@UtilityClass
public class HudDelta {
	private float sinceRefresh;
	private boolean spent = true;

	// HUD cache off: the next refresh owes nothing to earlier frames
	public void idle() {
		spent = true;
	}

	// once per cached-HUD frame, before the HUD is drawn; delta is this frame's own
	public void frame(boolean refresh, float delta) {
		sinceRefresh = (spent ? 0 : sinceRefresh) + delta;
		spent = refresh;
	}

	public float sinceRefresh() {
		return sinceRefresh;
	}
}
