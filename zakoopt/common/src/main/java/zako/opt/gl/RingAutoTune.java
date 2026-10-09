package zako.opt.gl;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;

import java.util.Arrays;

// the immediate ring wins a few percent on fast buses and can halve FPS where the GPU reads mapped memory over a slow
// PCIe link, so it is measured once per session right after spawning: short windows alternating with and without it
@UtilityClass
@Slf4j(topic = "zakoopt")
public class RingAutoTune {
	private final long FIRST_DELAY = 3_000_000_000L;
	private final long NOISY_RETRY = 10_000_000_000L;
	private final long WINDOW = 250_000_000L;
	private final int WINDOWS = 25;
	private final int SKIP = 5;
	private final double MIN_GAIN = 0.98;
	// interquartile spread of the per-window ratios above which the scene changed under the test
	private final double MAX_SPREAD = 0.10;
	private final double[] MEANS = new double[WINDOWS];
	// zakobench's background window never grabs the mouse
	private final boolean BENCH = "second".equals(System.getProperty("zakobench.monitor"));
	private boolean testing;
	private boolean done;
	private boolean keep;
	private long result;
	private long delay = FIRST_DELAY;
	private long calmSince;
	private Object level;
	private long last;
	private long windowStart;
	private int window;
	private int frames;
	private long sum;

	// odd windows run with the ring, so each is compared with the even windows on both sides
	public boolean ringAllowed() {
		return testing ? window % 2 == 1 : keep;
	}

	public String status() {
		return done ? (keep ? "on" : "off") + " (" + result + "% frame time)" : testing ? "testing" : "test pending";
	}

	// once per frame
	public void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || done) {
			return;
		}
		long now = System.nanoTime();
		long dt = now - last;
		last = now;
		// a grabbed mouse means plain play: no menu, chat, captcha or hub GUI, and the window is focused
		if (mc.level == null || mc.level != level || !(mc.mouseHandler.isMouseGrabbed() || BENCH)) {
			level = mc.level;
			testing = false;
			calmSince = now;
			return;
		}
		if (!testing) {
			if (now - calmSince >= delay) {
				testing = true;
				window = 0;
				frames = 0;
				sum = 0;
				windowStart = now;
			}
			return;
		}
		// the first frames after a switch still carry the previous mode's work
		if (++frames > SKIP) {
			sum += dt;
		}
		if (now - windowStart >= WINDOW) {
			MEANS[window++] = (double) sum / Math.max(1, frames - SKIP);
			frames = 0;
			sum = 0;
			windowStart = now;
			if (window == WINDOWS) {
				decide();
				testing = false;
				calmSince = now;
			}
		}
	}

	private void decide() {
		// frame time with the ring over the mean of its two neighbours without it: cancels any linear drift of the scene
		double[] ratios = new double[WINDOWS / 2];
		for (int i = 1; i < WINDOWS - 1; i += 2) {
			ratios[i / 2] = MEANS[i] / ((MEANS[i - 1] + MEANS[i + 1]) / 2);
		}
		Arrays.sort(ratios);
		double median = ratios[ratios.length / 2];
		double spread = ratios[ratios.length * 3 / 4] - ratios[ratios.length / 4];
		if (spread > MAX_SPREAD) {
			delay = NOISY_RETRY;
			log.info("Immediate ring test too noisy (spread {}), retrying later", Math.round(spread * 100));
			return;
		}
		keep = median < MIN_GAIN;
		result = Math.round(median * 100);
		done = true;
		log.info("Immediate ring test: {}% frame time with it -> {}", Math.round(median * 100), keep ? "kept" : "off");
	}
}
