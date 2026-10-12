package zako.opt.gl;

import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL33C;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// GPU time between named points of the frame, while F3 is open: each mark ends the running segment and starts one under its
// name. Elapsed-time queries, not timestamps: AMD gives a 3.2 context, and its glQueryCounter crashed the driver. Read three
// frames later, so nothing waits on the GPU
@UtilityClass
public class GpuTimers {
	private final int SLOTS = 4;
	private final int MARKS = 64;

	private boolean supported, checked, active, shown;
	private int slot;
	private final int[][] queries = new int[SLOTS][];
	private final String[][] names = new String[SLOTS][MARKS];
	private final int[] counts = new int[SLOTS];
	private final boolean[] pending = new boolean[SLOTS];

	private final Object2LongLinkedOpenHashMap<String> totals = new Object2LongLinkedOpenHashMap<>();
	private long frameTotal, frames, windowStart, lastFrameNs, cpuFrameNs, cpuFrames;
	private double utilization;
	private List<String> lines = List.of();

	// read while the GUI is extracted, which on 26.x is outside the frame's own timing
	public List<String> lines() {
		return shown ? lines : List.of();
	}

	public double utilization() {
		return utilization;
	}

	public void beginFrame() {
		active = Minecraft.getInstance().getDebugOverlay().showDebugScreen() && supported();
		shown = active;
		if (!active) {
			lastFrameNs = 0;
			return;
		}
		long now = System.nanoTime();
		if (lastFrameNs != 0) {
			cpuFrameNs += now - lastFrameNs;
			cpuFrames++;
		}
		lastFrameNs = now;
		slot = (slot + 1) % SLOTS;
		harvest(slot);
		counts[slot] = 0;
		mark("before world");
	}

	public void mark(String name) {
		if (!active || counts[slot] == MARKS) {
			return;
		}
		if (counts[slot] > 0) {
			GL15C.glEndQuery(GL33C.GL_TIME_ELAPSED);
		}
		int i = counts[slot]++;
		names[slot][i] = name;
		GL15C.glBeginQuery(GL33C.GL_TIME_ELAPSED, queries[slot][i]);
	}

	public void endFrame() {
		if (!active) {
			return;
		}
		if (counts[slot] > 0) {
			GL15C.glEndQuery(GL33C.GL_TIME_ELAPSED);
		}
		pending[slot] = true;
		// a mark after the end of the frame would start a query nothing ends
		active = false;
		window();
	}

	public boolean supported() {
		if (!checked) {
			checked = true;
			supported = GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query;
			if (supported) {
				for (int s = 0; s < SLOTS; s++) {
					queries[s] = new int[MARKS];
					GL15C.glGenQueries(queries[s]);
				}
			}
		}
		return supported;
	}

	private void harvest(int s) {
		if (!pending[s]) {
			return;
		}
		pending[s] = false;
		int n = counts[s];
		if (n == 0 || GL15C.glGetQueryObjecti(queries[s][n - 1], GL15C.GL_QUERY_RESULT_AVAILABLE) == 0) {
			return;
		}
		for (int i = 0; i < n; i++) {
			long ns = Integer.toUnsignedLong(GL15C.glGetQueryObjecti(queries[s][i], GL15C.GL_QUERY_RESULT));
			totals.addTo(names[s][i], ns);
			frameTotal += ns;
		}
		frames++;
	}

	private void window() {
		long now = System.nanoTime();
		if (windowStart == 0) {
			windowStart = now;
		}
		if (now - windowStart < 1_000_000_000L || frames == 0) {
			return;
		}
		List<Map.Entry<String, Long>> sorted = new ArrayList<>(totals.entrySet());
		sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Long> e : sorted) {
			double ms = e.getValue() / 1e6 / frames;
			if (ms >= 0.05) {
				parts.add("%s %.2f".formatted(e.getKey(), ms));
			}
		}
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder("GPU %.2f ms/frame:".formatted(frameTotal / 1e6 / frames));
		for (String part : parts) {
			if (line.length() + part.length() > 110) {
				out.add(line.toString());
				line = new StringBuilder(" ");
			}
			line.append(' ').append(part).append(',');
		}
		out.add(line.substring(0, line.length() - 1));
		lines = out;
		utilization = cpuFrames == 0 ? 0 : Math.min(100, (double) frameTotal / frames * 100 / ((double) cpuFrameNs / cpuFrames));
		totals.clear();
		frameTotal = frames = cpuFrameNs = cpuFrames = 0;
		windowStart = now;
	}
}
