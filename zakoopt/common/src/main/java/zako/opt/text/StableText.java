package zako.opt.text;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import zako.opt.ZakoOptConfig;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class StableText implements FormattedCharSequence {
	private static final Set<FormattedCharSequence> STABLE = Collections.newSetFromMap(new WeakHashMap<>());
	FormattedCharSequence text;

	public static FormattedCharSequence mark(FormattedCharSequence text) {
		StableText stable = new StableText(text);
		synchronized (STABLE) {
			STABLE.add(stable);
		}
		return stable;
	}

	public static boolean isStable(FormattedCharSequence text) {
		if (ZakoOptConfig.microOpts2()) {
			return text instanceof StableText;
		}
		synchronized (STABLE) {
			return STABLE.contains(text);
		}
	}

	@Override
	public boolean accept(FormattedCharSink sink) {
		return text.accept(sink);
	}
}
