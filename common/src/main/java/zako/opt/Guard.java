package zako.opt;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import zako.opt.feature.Feature;
import zako.opt.feature.Features;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j(topic = "zakoopt")
@UtilityClass
public class Guard {
	// dev runs from Gradle crash on our errors instead, so they get seen; -Dzakoopt.guard.crash=true does the same in a real game
	private final boolean CRASH = Platform.isDevelopment() || Boolean.getBoolean("zakoopt.guard.crash");
	private final Set<String> FAILED = ConcurrentHashMap.newKeySet();
	// features turned off since the player was last told; told in chat once a world is open
	private final List<Feature> UNTOLD = new CopyOnWriteArrayList<>();
	private volatile boolean untold;

	public void fail(Throwable t, String mixin, String method) {
		if (CRASH) {
			if (t instanceof RuntimeException r) {
				throw r;
			}
			throw (Error) t;
		}
		if (!FAILED.add(mixin + "#" + method)) {
			return;
		}
		String name = mixin.substring(mixin.indexOf(".mixin.") + ".mixin.".length());
		List<Feature> off = Features.all().stream().filter(f -> Arrays.asList(f.data().mixins()).contains(name)).toList();
		off.forEach(f -> f.broken = true);
		log.error("{}.{} failed; {}", name, method, off.isEmpty() ? "the call goes on without it" : "turned off until restart: "
				+ off.stream().map(Feature::key).toList(), t);
		off.stream().filter(f -> !UNTOLD.contains(f)).forEach(UNTOLD::add);
		untold = true;
	}

	// once a frame on the render thread: one chat line per batch of failures, asking the player for the log
	public void tell() {
		if (!untold || !Chat.ready()) {
			return;
		}
		untold = false;
		List<Feature> off = List.copyOf(UNTOLD);
		UNTOLD.removeAll(off);
		MutableComponent names = Component.empty();
		for (int i = 0; i < off.size(); i++) {
			if (i > 0) {
				names.append(", ");
			}
			names.append(Component.translatable("zakoopt.option." + off.get(i).key()));
		}
		Chat.show(off.isEmpty() ? Component.translatable("zakoopt.guard.chat.other") : Component.translatable("zakoopt.guard.chat", names));
	}

	public List<String> lines() {
		List<String> broken = Features.all().stream().filter(f -> f.broken).map(Feature::key).toList();
		return broken.isEmpty() ? List.of() : List.of("ZakoOpt errors, off until restart: " + String.join(", ", broken));
	}
}
