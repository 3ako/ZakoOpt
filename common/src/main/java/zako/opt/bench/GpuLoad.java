package zako.opt.bench;

import lombok.experimental.UtilityClass;
import zako.opt.gl.GpuTimers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// an elapsed-time query over the frame spans the GPU's idle gaps too, so a frame held up by the CPU reads as a busy GPU;
// on Windows the OS counter Task Manager shows is read instead, from one typeperf sampling this process once a second
@UtilityClass
public class GpuLoad {
	private volatile double value = -1;
	private volatile boolean failed;
	private boolean started;

	public double get() {
		if (!started) {
			started = true;
			start();
		}
		return failed ? GpuTimers.utilization() : value;
	}

	private void start() {
		if (!System.getProperty("os.name", "").startsWith("Windows")) {
			failed = true;
			return;
		}
		Thread t = new Thread(() -> {
			try {
				Process p = new ProcessBuilder("typeperf", "\\GPU Engine(pid_" + ProcessHandle.current().pid() + "_*engtype_3D)\\Utilization Percentage", "-si", "1")
						.redirectErrorStream(true).start();
				Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
				try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = in.readLine()) != null) {
						// "time","engine 0","engine 1",...; the header row and typeperf's own messages fail to parse and are skipped
						String[] cells = line.split("\",\"");
						double sum = 0;
						int parsed = 0;
						for (int i = 1; i < cells.length; i++) {
							try {
								sum += Double.parseDouble(cells[i].replace("\"", "").trim());
								parsed++;
							} catch (NumberFormatException ignored) {
							}
						}
						if (parsed > 0) {
							value = Math.min(100, sum);
						}
					}
				}
			} catch (Exception ignored) {
			}
			failed = true;
		}, "ZakoOpt GPU load");
		t.setDaemon(true);
		t.start();
	}
}
