package zako.opt.text;

import zako.opt.feature.Features;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

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
		if (Features.MICRO_OPTS.stableResults) {
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
