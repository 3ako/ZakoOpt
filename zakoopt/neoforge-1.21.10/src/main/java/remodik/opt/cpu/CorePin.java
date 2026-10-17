package remodik.opt.cpu;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

// the hot threads onto the best cores: the CCD with the big L3 on an X3D, the P-cores on a hybrid Intel, the big cores on a
// phone. A thread can only move itself, so each hot thread calls check() now and then and follows the latest setting
@Slf4j(topic = "zakoopt")
@UtilityClass
public class CorePin {
	private final int WINDOWS_CACHE_RELATION = 2;
	private final int POWER_THROTTLING = 4;
	private final ThreadLocal<int[]> APPLIED = ThreadLocal.withInitial(() -> new int[]{0});
	private Kernel32 kernel;
	private Libc libc;
	private long best;
	private long all;
	private String why;
	private volatile boolean on;
	private volatile int generation = 1;

	interface Kernel32 extends Library {
		Pointer GetCurrentThread();

		Pointer GetCurrentProcess();

		long SetThreadAffinityMask(Pointer thread, long mask);

		int GetCurrentProcessorNumber();

		boolean GetProcessAffinityMask(Pointer process, LongByReference processMask, LongByReference systemMask);

		boolean GetLogicalProcessorInformationEx(int relation, Pointer buffer, IntByReference length);

		boolean GetSystemCpuSetInformation(Pointer info, int length, IntByReference returned, Pointer process, int flags);

		boolean SetProcessInformation(Pointer process, int infoClass, Pointer info, int size);
	}

	interface Libc extends Library {
		int sched_setaffinity(int pid, long size, long[] mask);

		int sched_getaffinity(int pid, long size, long[] mask);

		int sched_getcpu();
	}

	static {
		try {
			if (Platform.isWindows()) {
				kernel = Native.load("kernel32", Kernel32.class);
				LongByReference process = new LongByReference(), system = new LongByReference();
				if (!kernel.GetProcessAffinityMask(kernel.GetCurrentProcess(), process, system)) {
					throw new IllegalStateException("GetProcessAffinityMask failed, error " + Native.getLastError());
				}
				all = process.getValue();
				findWindows();
			} else if (Platform.isLinux() || Platform.isAndroid()) {
				libc = Native.load("c", Libc.class);
				long[] mask = new long[16];
				if (libc.sched_getaffinity(0, mask.length * 8L, mask) != 0) {
					throw new IllegalStateException("sched_getaffinity failed, errno " + Native.getLastError());
				}
				all = mask[0];
				findLinux();
			}
			// -Dzakoopt.corepin.mask=0xFF picks the cores by hand, for checking a machine whose cores all look alike
			String forced = System.getProperty("zakoopt.corepin.mask");
			if (forced != null) {
				best = Long.decode(forced);
				why = "set by hand";
			}
			best &= all;
			if (best == 0 || best == all) {
				best = 0;
			}
		} catch (Throwable t) {
			log.warn("Can't read the CPU layout, best-core pinning is off", t);
			best = 0;
		}
		log.info("Best cores: {}", best == 0 ? "none, all cores alike" : mask(best) + " (" + why + ")");
	}

	public boolean available() {
		return best != 0;
	}

	// the render thread hands the setting over every frame
	public void set(boolean pin) {
		if (pin != on) {
			on = pin;
			generation++;
			if (kernel != null) {
				powerThrottling(!pin);
			}
		}
	}

	public void check() {
		int[] applied = APPLIED.get();
		int g = generation;
		if (applied[0] == g || best == 0) {
			return;
		}
		applied[0] = g;
		long mask = on ? best : all;
		if (kernel != null) {
			kernel.SetThreadAffinityMask(kernel.GetCurrentThread(), mask);
		} else if (libc != null) {
			libc.sched_setaffinity(0, 8, new long[]{mask});
		}
	}

	public String status() {
		if (best == 0) {
			return "n/a";
		}
		if (!on) {
			return "all (best " + mask(best) + ", " + why + ")";
		}
		int cpu = kernel != null ? kernel.GetCurrentProcessorNumber() : libc.sched_getcpu();
		return "best " + mask(best) + " (" + why + "), render on " + cpu;
	}

	// X3D: the cores under the biggest L3; hybrid Intel: the highest efficiency class, which Windows gives the P-cores
	private void findWindows() {
		IntByReference length = new IntByReference();
		kernel.GetLogicalProcessorInformationEx(WINDOWS_CACHE_RELATION, null, length);
		Memory buffer = new Memory(length.getValue());
		if (kernel.GetLogicalProcessorInformationEx(WINDOWS_CACHE_RELATION, buffer, length)) {
			// SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX: Size at 4, CACHE_RELATIONSHIP from 8: Level 8, CacheSize 12, Group mask 40, group 48
			TreeMap<Long, Long> l3 = new TreeMap<>();
			for (long at = 0; at < length.getValue(); at += buffer.getInt(at + 4)) {
				if (buffer.getByte(at + 8) == 3 && buffer.getShort(at + 48) == 0) {
					l3.merge(buffer.getInt(at + 12) & 0xFFFFFFFFL, buffer.getLong(at + 40), (a, b) -> a | b);
				}
			}
			if (l3.size() > 1) {
				Map.Entry<Long, Long> biggest = l3.lastEntry();
				best = biggest.getValue();
				why = "L3 " + (biggest.getKey() >> 20) + " MB";
				return;
			}
		}
		IntByReference returned = new IntByReference();
		kernel.GetSystemCpuSetInformation(null, 0, returned, kernel.GetCurrentProcess(), 0);
		if (returned.getValue() == 0) {
			return;
		}
		Memory sets = new Memory(returned.getValue());
		if (!kernel.GetSystemCpuSetInformation(sets, returned.getValue(), returned, kernel.GetCurrentProcess(), 0)) {
			return;
		}
		// SYSTEM_CPU_SET_INFORMATION: Size 0, Type 4, Group 12, LogicalProcessorIndex 14, EfficiencyClass 18
		TreeMap<Integer, Long> classes = new TreeMap<>();
		for (long at = 0; at < returned.getValue(); at += sets.getInt(at)) {
			if (sets.getInt(at + 4) == 0 && sets.getShort(at + 12) == 0) {
				classes.merge(sets.getByte(at + 18) & 0xFF, 1L << sets.getByte(at + 14), (a, b) -> a | b);
			}
		}
		if (classes.size() > 1) {
			best = classes.lastEntry().getValue();
			why = "performance cores";
		}
	}

	// the same two answers from sysfs: the biggest L3, else all but the lowest top clock (the big cores of a phone)
	private void findLinux() {
		TreeMap<Long, Long> l3 = new TreeMap<>(), clock = new TreeMap<>();
		for (int cpu = 0; cpu < 64; cpu++) {
			Path dir = Path.of("/sys/devices/system/cpu/cpu" + cpu);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			long size = number(dir.resolve("cache/index3/size")), freq = number(dir.resolve("cpufreq/cpuinfo_max_freq"));
			if (size > 0) {
				l3.merge(size, 1L << cpu, (a, b) -> a | b);
			}
			if (freq > 0) {
				clock.merge(freq, 1L << cpu, (a, b) -> a | b);
			}
		}
		if (l3.size() > 1) {
			best = l3.lastEntry().getValue();
			why = "L3 " + (l3.lastKey() >> 10) + " MB";
		} else if (clock.size() > 1) {
			// everything but the slowest cluster: a phone may have a single prime core, too few for the render thread and the workers
			best = all & ~clock.firstEntry().getValue();
			why = "big cores";
		}
	}

	private long number(Path file) {
		try {
			String s = Files.readString(file).trim().toUpperCase();
			return s.endsWith("K") ? Long.parseLong(s.substring(0, s.length() - 1)) : Long.parseLong(s);
		} catch (Exception e) {
			return 0;
		}
	}

	// Windows 11 moves a process it thinks is idle to the efficient cores (EcoQoS), e.g. when the window loses focus
	private void powerThrottling(boolean allowed) {
		// PROCESS_POWER_THROTTLING_STATE: Version 1, ControlMask EXECUTION_SPEED when we decide, StateMask 0 = never throttle
		Memory state = new Memory(12);
		state.setInt(0, 1);
		state.setInt(4, allowed ? 0 : 1);
		state.setInt(8, 0);
		kernel.SetProcessInformation(kernel.GetCurrentProcess(), POWER_THROTTLING, state, 12);
	}

	private String mask(long mask) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 64; i++) {
			if ((mask >> i & 1) != 0) {
				int j = i;
				while (j < 63 && (mask >> j + 1 & 1) != 0) {
					j++;
				}
				s.append(s.isEmpty() ? "" : ",").append(i == j ? String.valueOf(i) : i + "-" + j);
				i = j;
			}
		}
		return s.toString();
	}
}
