package zako.opt;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import zako.opt.feature.Feature;
import zako.opt.feature.Features;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j(topic = "zakoopt")
@UtilityClass
public class Guard {
	private final Set<String> FAILED = ConcurrentHashMap.newKeySet();

	public void fail(Throwable t, String mixin, String method) {
		if (!FAILED.add(mixin + "#" + method)) {
			return;
		}
		String name = mixin.substring(mixin.indexOf(".mixin.") + ".mixin.".length());
		List<Feature> off = Features.all().stream().filter(f -> Arrays.asList(f.data().mixins()).contains(name)).toList();
		off.forEach(f -> f.broken = true);
		log.error("{}.{} failed; {}", name, method, off.isEmpty() ? "the call goes on without it" : "turned off until restart: "
				+ off.stream().map(Feature::key).toList(), t);
	}

	public List<String> lines() {
		List<String> broken = Features.all().stream().filter(f -> f.broken).map(Feature::key).toList();
		return broken.isEmpty() ? List.of() : List.of("ZakoOpt errors, off until restart: " + String.join(", ", broken));
	}
}
