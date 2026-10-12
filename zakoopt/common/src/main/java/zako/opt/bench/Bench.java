package zako.opt.bench;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import zako.opt.Blocked;
import zako.opt.ZakoOptConfig;
import zako.opt.ZakoOptions;
import zako.opt.update.UpdateUi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

// the benchmark runs as a script on its own thread: it builds a temporary flat world, grows the scene until FPS drops to
// the target, measures the switches one by one, then deletes the world. The game keeps rendering in between, which is what gets counted
@Slf4j(topic = "zakoopt")
@UtilityClass
public class Bench {
	private final String WORLD = "zakoopt-benchmark";
	private final int SETTLE_MS = 500;
	private final int SEGMENT_MS = 1500;
	private final double MIN_GAIN = 0.03;
	// switches whose worth depends on the device and that leave the picture as it is; LODs and the like stay the player's call
	private final List<Test> TESTS = List.of(new Test("lunar_multidraw", "terrain"), new Test("immediate_ring", "overview"), new Test("ring_zero_copy", "overview"), new Test("frame_fence", "overview"),
			new Test("lazy_clear", "overview"), new Test("fbo_share", "overview"), new Test("hud_cache", "overview"),
			new Test("micro_opts", "overview"), new Test("lookup_caches", "overview"), new Test("parallel_models", "crowd"), new Test("no_entity_sort", "crowd"),
			new Test("writer_cache", "crowd"), new Test("outline_skip", "crowd"), new Test("display_cull", "crowd"), new Test("state_pool", "crowd"), new Test("core_pin", "crowd"), new Test("no_item_sort", "items"), new Test("resting_items", "items"), new Test("block_entity_cache", "blocks"),
			new Test("block_entity_early_cull", "blocks"), new Test("sign_cache", "blocks"), new Test("spawner_tick_cache", "blocks"), new Test("spawner_replay", "blocks"), new Test("moving_block_cache", "pistons"),
			new Test("prepared_text_cache", "holograms"), new Test("bidi_cache", "holograms"), new Test("parallel_particles", "holograms"));
	private final long[] FRAME_NS = new long[1 << 16];
	private volatile int frames;
	private long lastFrame;
	private volatile boolean running;
	private volatile boolean joined;
	private volatile boolean cancelled;
	private volatile Status status = new Status(Component.empty(), 0);

	public record Status(Component stage, int percent) {
	}

	private static final class Aborted extends RuntimeException {
	}

	private record Test(String key, String view) {
	}

	private record Result(int level, double modGain, Map<String, Double> off, Map<String, Boolean> chosen, Map<String, Object> picks) {
	}

	public boolean running() {
		return running;
	}

	public Status status() {
		return status;
	}

	public void cancel() {
		cancelled = true;
	}

	public void frame(long now) {
		if (running && lastFrame != 0) {
			FRAME_NS[frames & FRAME_NS.length - 1] = now - lastFrame;
			frames++;
		}
		lastFrame = now;
	}

	public void confirm(Screen parent) {
		UpdateUi.setScreen(new ConfirmScreen(yes -> {
			if (yes) {
				start(parent);
			} else {
				UpdateUi.setScreen(parent);
			}
		}, Component.translatable("zakoopt.bench.title"), Component.translatable("zakoopt.bench.intro"),
				Component.translatable("zakoopt.bench.start"), Component.translatable("gui.cancel")));
	}

	private void start(Screen parent) {
		running = true;
		joined = false;
		cancelled = false;
		status = new Status(Component.empty(), 0);
		Minecraft mc = Minecraft.getInstance();
		boolean pause = mc.options.pauseOnLostFocus;
		mc.options.pauseOnLostFocus = false;
		ZakoOptConfig.Values saved = copy(ZakoOptConfig.values, new ZakoOptConfig.Values());
		Thread thread = new Thread(() -> {
			Result result = null;
			try {
				result = run(mc, parent, saved);
			} catch (Aborted e) {
				log.info("Benchmark cancelled");
			} catch (Exception e) {
				log.error("Benchmark failed", e);
			} finally {
				Result shown = result;
				main(mc, () -> {
					if (mc.level != null) {
						BenchWorld.leave(mc);
					}
					delete(mc);
					mc.options.pauseOnLostFocus = pause;
					copy(saved, ZakoOptConfig.values);
					UpdateUi.setScreen(shown == null ? parent : report(shown, parent));
					log.info("Benchmark finished");
					return null;
				});
				running = false;
			}
		}, "ZakoOpt benchmark");
		thread.setDaemon(true);
		thread.start();
	}

	private Result run(Minecraft mc, Screen parent, ZakoOptConfig.Values saved) {
		main(mc, () -> {
			delete(mc);
			mc.createWorldOpenFlows().createFreshLevel(WORLD, BenchWorld.settings(WORLD), new WorldOptions(7L, false, false),
					registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value().createWorldDimensions(), parent);
			return null;
		});
		long deadline = System.currentTimeMillis() + 120_000;
		while (mc.player == null || mc.getSingleplayerServer() == null) {
			if (System.currentTimeMillis() > deadline) {
				throw new IllegalStateException("benchmark world did not load");
			}
			sleep(200);
		}
		joined = true;
		main(mc, () -> {
			Scene.terrainAt(mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
			UpdateUi.setScreen(new ProgressScreen());
			return null;
		});
		List<Test> tests = TESTS.stream().filter(t -> toggle(t.key()) != null).toList();
		// measured from the defaults; the ring is held where it is set so its own auto-tune does not flip it mid-measurement
		ZakoOptConfig.Values defaults = new ZakoOptConfig.Values();
		main(mc, () -> {
			copy(defaults, ZakoOptConfig.values);
			ZakoOptConfig.values.immediateRingForce = true;
			return null;
		});

		// over the scene first: a fill into a chunk the player has not loaded yet fails as a whole, and quietly
		Scene.View o = Scene.OVERVIEW;
		server(mc, tp(o));
		sleep(3000);
		List<String> setup = new ArrayList<>(Scene.base());
		setup.addAll(tp(o));
		server(mc, setup);
		sleep(2000);

		settle(mc);
		double base = fps(mc, 3000);
		double target = Math.max(Math.min(base / 2, 144), 30);
		log.info("Benchmark: empty scene {} fps, target {}", Math.round(base), Math.round(target));
		// frame time grows a bit faster than linearly with load: probe, extrapolate to 80% of the target, jump at most x3 at a time
		double t0 = 1000 / base, targetMs = 1000 / target;
		int level = 0, next = 2, step = 0;
		double fps = base;
		while (true) {
			step++;
			progress(mc, Component.translatable("zakoopt.bench.ramp", step, Math.round(fps), Math.round(target)), step * 10 / (step + 1));
			List<String> add = new ArrayList<>();
			for (int lv = level; lv < next; lv++) {
				add.addAll(Scene.level(lv));
			}
			server(mc, add);
			level = next;
			sleep(1000);
			fps = fps(mc, 2000);
			log.info("Benchmark: level {} {} fps", level, Math.round(fps));
			double t = 1000 / fps;
			if (t >= targetMs * 0.85 || level == Scene.LEVELS) {
				break;
			}
			double k = Math.max((t - t0) / level, 1e-6);
			next = Math.min(Scene.LEVELS, Math.min(3 * level, Math.max(level + 1, (int) Math.round(0.8 * (targetMs - t0) / k))));
		}

		// one switch at a time on the view it shows on, on-off-off-on so a drifting frame rate cancels out; a switch goes off only
		// if both pairs agree it is in the way, and later switches are measured with the earlier verdicts in place
		Map<String, Double> off = new LinkedHashMap<>();
		Map<String, Object> picks = new LinkedHashMap<>();
		String view = null;
		for (int i = 0; i < tests.size(); i++) {
			Test test = tests.get(i);
			ZakoOptions.Toggle toggle = toggle(test.key());
			if (test.key().equals("ring_zero_copy") && !ZakoOptConfig.values.immediateRing) {
				continue;
			}
			if (!test.view().equals(view)) {
				view = test.view();
				Scene.View v = Scene.views().stream().filter(x -> x.key().equals(test.view())).findFirst().orElseThrow();
				server(mc, tp(v));
				sleep(2000);
				server(mc, tp(v));
				if (v.key().equals("terrain")) {
					settle(mc);
				}
				// the look-versus-speed settings are the player's to pick, over the part of the scene they show on
				List<String> pick = switch (view) {
					case "crowd" -> Scene.MANNEQUIN ? List.of("entity_lod_distance", "player_lod_distance") : List.of("entity_lod_distance");
					case "items" -> List.of("item_single_copy");
					case "blocks" -> List.of("spawner_cull", "spawner_distance");
					default -> List.of();
				};
				Scene.View at = switch (view) {
					case "crowd" -> Scene.LOD;
					case "items" -> Scene.ITEMS_CLOSE;
					default -> Scene.SPAWNERS;
				};
				if (pick(mc, saved, pick, at, "zakoopt.bench." + at.key(), picks)) {
					server(mc, tp(v));
					sleep(2000);
				}
			}
			Component name = Component.translatable("zakoopt.option." + test.key());
			int percent = 10 + i * 85 / tests.size();
			// off/on frame rate; a switch on by default goes off, one off by default goes on, only if two runs agree it pays
			boolean byDefault = toggle.get().get();
			double ratio = abba(mc, test, toggle, name, i, tests.size(), percent);
			if (byDefault ? ratio >= 1 + MIN_GAIN : ratio <= 1 / (1 + MIN_GAIN)) {
				double again = abba(mc, test, toggle, name, i, tests.size(), percent);
				ratio = byDefault ? Math.min(ratio, again) : Math.max(ratio, again);
			}
			boolean keep = byDefault ? ratio < 1 + MIN_GAIN : ratio <= 1 / (1 + MIN_GAIN);
			log.info("Benchmark: {} -> {}", test.key(), keep ? "on" : "off");
			if (byDefault && !keep) {
				off.put(test.key(), ratio - 1);
			}
			main(mc, () -> {
				toggle.set().accept(keep);
				return null;
			});
		}

		server(mc, tp(o));
		sleep(2000);
		server(mc, tp(o));
		progress(mc, Component.translatable("zakoopt.bench.final"), 97);
		Map<String, Boolean> chosen = new LinkedHashMap<>();
		tests.forEach(t -> chosen.put(t.key(), toggle(t.key()).get().get()));
		// the whole mod as picked against no mod at all
		double with1 = master(mc, true), without1 = master(mc, false), without2 = master(mc, false), with2 = master(mc, true);
		double modGain = (with1 + with2) / (without1 + without2) - 1;
		log.info("Benchmark: level {}, mod {}%, off {}", level, Math.round(modGain * 100), off);

		CompletableFuture<Void> compared = new CompletableFuture<>();
		main(mc, () -> {
			UpdateUi.setScreen(new CompareScreen(compared));
			return null;
		});
		waitWhile(() -> !compared.isDone());
		return new Result(level, modGain, off, chosen, picks);
	}

	// starts from the player's own values; false when none of the settings exists on this version
	private boolean pick(Minecraft mc, ZakoOptConfig.Values saved, List<String> keys, Scene.View at, String hint, Map<String, Object> picks) {
		List<ZakoOptions.Option> options = keys.stream().map(Bench::option).filter(o -> o != null && Blocked.reason(o.key()) == null).toList();
		if (options.isEmpty()) {
			return false;
		}
		// there first: the pick's own blocks would not go into chunks the player has not loaded
		server(mc, tp(at));
		sleep(2000);
		server(mc, tp(at));
		if (at == Scene.LOD) {
			server(mc, Scene.ruler(Component.translatable("zakoopt.blocks", "%d").getString()));
		}
		if (at == Scene.SPAWNERS) {
			server(mc, Scene.spawnerLabels(Component.translatable("zakoopt.blocks", "%d").getString()));
		}
		if (at == Scene.SPAWNERS) {
			growSpawners(mc, (ZakoOptions.Toggle) options.getFirst());
		}
		if (at == Scene.ITEMS_CLOSE) {
			growPile(mc, (ZakoOptions.Toggle) options.getFirst());
		}
		CompletableFuture<Void> done = new CompletableFuture<>();
		main(mc, () -> {
			ZakoOptConfig.Values now = copy(ZakoOptConfig.values, new ZakoOptConfig.Values());
			copy(saved, ZakoOptConfig.values);
			Map<String, Object> start = new LinkedHashMap<>();
			options.forEach(o -> start.put(o.key(), value(o)));
			copy(now, ZakoOptConfig.values);
			options.forEach(o -> set(o, start.get(o.key())));
			UpdateUi.setScreen(new PickScreen(options, hint, done));
			return null;
		});
		waitWhile(() -> !done.isDone());
		List<String> clear = new ArrayList<>(List.of("tp @e[tag=" + Scene.RULER + "] -7 -300 0", "kill @e[tag=" + Scene.RULER + "]", "kill @e[tag=" + Scene.PILE + "]"));
		if (at == Scene.SPAWNERS) {
			clear.addAll(Scene.clearSpawnerField());
		}
		server(mc, clear);
		main(mc, () -> {
			UpdateUi.setScreen(new ProgressScreen());
			return null;
		});
		options.forEach(o -> picks.put(o.key(), value(o)));
		log.info("Benchmark: picked {}", picks);
		return true;
	}

	// the scene's own items barely show the copies; full stacks are added under the camera, drawn with copies, until the frame
	// rate is down by about a third, so switching shows on this device without sinking a weak one
	private void growPile(Minecraft mc, ZakoOptions.Toggle singleCopy) {
		main(mc, () -> {
			singleCopy.set().accept(false);
			return null;
		});
		sleep(1000);
		double start = fps(mc, 1000);
		for (int batch = 0; batch < 10; batch++) {
			server(mc, Scene.pile(batch));
			sleep(SETTLE_MS);
			double now = fps(mc, 1000);
			log.info("Benchmark: item pile {} stacks {} fps", (batch + 1) * 200, Math.round(now));
			if (now < start * 0.7) {
				break;
			}
		}
	}

	// the scene's spawners are too few to show; strips are added under the camera, every mob drawn, until the frame rate is
	// down by about a third
	private void growSpawners(Minecraft mc, ZakoOptions.Toggle cull) {
		main(mc, () -> {
			cull.set().accept(false);
			return null;
		});
		sleep(1000);
		double start = fps(mc, 1000);
		for (int strip = 0; strip < Scene.SPAWNER_STRIPS; strip++) {
			server(mc, Scene.spawnerStrip(strip));
			sleep(SETTLE_MS);
			double now = fps(mc, 1000);
			log.info("Benchmark: spawner field {} spawners {} fps", (strip + 1) * 100, Math.round(now));
			if (now < start * 0.7) {
				break;
			}
		}
	}

	private Object value(ZakoOptions.Option option) {
		return switch (option) {
			case ZakoOptions.Toggle t -> t.get().get();
			case ZakoOptions.Distance d -> d.get().get();
		};
	}

	private void set(ZakoOptions.Option option, Object value) {
		switch (option) {
			case ZakoOptions.Toggle t -> t.set().accept((Boolean) value);
			case ZakoOptions.Distance d -> d.set().accept((Integer) value);
		}
	}

	// how much faster the game runs with the switch off than on; 1 when the two pairs disagree on which is faster
	private double abba(Minecraft mc, Test test, ZakoOptions.Toggle toggle, Component name, int i, int n, int percent) {
		double on1 = measure(mc, toggle, true, name, i, n, percent);
		double off1 = measure(mc, toggle, false, name, i, n, percent);
		double off2 = measure(mc, toggle, false, name, i, n, percent);
		double on2 = measure(mc, toggle, true, name, i, n, percent);
		log.info("Benchmark: {} on {}/{} off {}/{}", test.key(), Math.round(on1), Math.round(on2), Math.round(off1), Math.round(off2));
		return off1 > on1 == off2 > on2 ? (off1 + off2) / (on1 + on2) : 1;
	}

	private double measure(Minecraft mc, ZakoOptions.Toggle toggle, boolean on, Component name, int i, int n, int percent) {
		main(mc, () -> {
			toggle.set().accept(on);
			return null;
		});
		progress(mc, Component.translatable("zakoopt.bench.test", name, i + 1, n), percent);
		sleep(SETTLE_MS);
		return fps(mc, SEGMENT_MS);
	}

	private double master(Minecraft mc, boolean on) {
		main(mc, () -> {
			ZakoOptConfig.values.enabled = on;
			return null;
		});
		sleep(SETTLE_MS);
		return fps(mc, 2000);
	}

	// runs after the player's settings are back: only what the test would change is marked and applied
	private Screen report(Result r, Screen parent) {
		List<String> changes = new ArrayList<>(r.chosen().entrySet().stream().filter(e -> e.getValue() != (boolean) toggle(e.getKey()).get().get()).map(Map.Entry::getKey).toList());
		r.picks().forEach((key, value) -> {
			if (!value.equals(value(option(key)))) {
				changes.add(key);
			}
		});
		Component gain = Component.translatable("zakoopt.bench.gain", "%+d%%".formatted(Math.round(r.modGain() * 100)));
		return new ResultScreen(parent, gain, r.chosen(), r.picks(), changes, () -> {
			changes.forEach(key -> set(option(key), r.picks().containsKey(key) ? r.picks().get(key) : r.chosen().get(key)));
			ZakoOptConfig.save();
		});
	}

	private ZakoOptions.Toggle toggle(String key) {
		return option(key) instanceof ZakoOptions.Toggle t && Blocked.reason(key) == null ? t : null;
	}

	private ZakoOptions.Option option(String key) {
		for (ZakoOptions.Page page : ZakoOptions.PAGES) {
			for (ZakoOptions.Group group : page.groups()) {
				for (ZakoOptions.Option option : group.available()) {
					if (option.key().equals(key)) {
						return option;
					}
				}
			}
		}
		return null;
	}

	@SneakyThrows(IllegalAccessException.class)
	private ZakoOptConfig.Values copy(ZakoOptConfig.Values from, ZakoOptConfig.Values to) {
		for (Field f : ZakoOptConfig.Values.class.getFields()) {
			f.set(to, f.get(from));
		}
		return to;
	}

	// the perch is set again on every move: one placed with the scene could be gone by the time the camera gets there
	private List<String> tp(Scene.View v) {
		List<String> c = new ArrayList<>(Scene.zone(v));
		c.addAll(List.of("fill %d %d %d %d %d %d minecraft:barrier".formatted(v.x() - 1, v.y() - 1, v.z() - 1, v.x() + 1, v.y() - 1, v.z() + 1),
				"tp @a %d.5 %d %d.5 %d %d".formatted(v.x(), v.y(), v.z(), v.yaw(), v.pitch())));
		return c;
	}

	private void progress(Minecraft mc, Component what, int percent) {
		status = new Status(what, percent);
	}

	// the last second of frames, for the live readout over the pick controls
	public double recentFps() {
		int n = 0, last = frames;
		long sum = 0;
		long[] times = new long[Math.min(last, FRAME_NS.length)];
		while (n < times.length && sum < 1_000_000_000L) {
			times[n] = FRAME_NS[last - 1 - n & FRAME_NS.length - 1];
			sum += times[n++];
		}
		java.util.Arrays.sort(times, 0, n);
		return n == 0 ? 0 : 1e9 / times[n / 2];
	}

	// the land generates and meshes as the camera arrives: wait until two seconds in a row agree within 5%, at most 30 s
	private void settle(Minecraft mc) {
		long end = System.currentTimeMillis() + 30_000;
		double last = fps(mc, 1000);
		while (System.currentTimeMillis() < end) {
			double now = fps(mc, 1000);
			if (Math.abs(now - last) < last * 0.05) {
				return;
			}
			last = now;
		}
	}

	// the last n frame times, oldest first
	public long[] lastFrames(int n) {
		int last = frames;
		long[] times = new long[Math.min(n, Math.min(last, FRAME_NS.length))];
		for (int i = 0; i < times.length; i++) {
			times[i] = FRAME_NS[last - times.length + i & FRAME_NS.length - 1];
		}
		return times;
	}

	// from the median frame time: a GC pause or a chunk upload is one long frame and barely moves it
	private double fps(Minecraft mc, int ms) {
		waitWhile(mc::isPaused);
		int start = frames;
		sleep(ms);
		if (mc.isPaused()) {
			return fps(mc, ms);
		}
		int n = Math.min(frames - start, FRAME_NS.length);
		long[] times = new long[n];
		for (int i = 0; i < n; i++) {
			times[i] = FRAME_NS[frames - n + i & FRAME_NS.length - 1];
		}
		java.util.Arrays.sort(times);
		return n == 0 ? 0 : 1e9 / times[n / 2];
	}

	// from the render thread, without waiting for the server
	public void teleport(Scene.View view) {
		var server = Minecraft.getInstance().getSingleplayerServer();
		if (server != null) {
			server.execute(() -> tp(view).forEach(c -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c)));
		}
	}

	private void server(Minecraft mc, List<String> commands) {
		var server = mc.getSingleplayerServer();
		if (server == null) {
			throw new Aborted();
		}
		server.submit(() -> {
			var source = server.createCommandSourceStack().withSuppressedOutput();
			commands.forEach(c -> server.getCommands().performPrefixedCommand(source, c));
		}).join();
	}

	private <T> T main(Minecraft mc, Supplier<T> task) {
		return mc.submit(task).join();
	}

	private void delete(Minecraft mc) {
		if (!mc.getLevelSource().levelExists(WORLD)) {
			return;
		}
		try (var access = mc.getLevelSource().createAccess(WORLD)) {
			access.deleteLevel();
		} catch (Exception e) {
			log.warn("Could not delete the benchmark world", e);
		}
	}

	private void waitWhile(BooleanSupplier condition) {
		while (condition.getAsBoolean()) {
			sleep(200);
		}
	}

	// Esc on the progress panel or quitting the world cancels
	@SneakyThrows(InterruptedException.class)
	private void sleep(long ms) {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) {
			Thread.sleep(Math.min(100, end - System.currentTimeMillis() + 1));
			if (cancelled || joined && Minecraft.getInstance().level == null) {
				throw new Aborted();
			}
		}
	}
}
