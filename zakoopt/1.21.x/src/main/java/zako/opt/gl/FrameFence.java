package zako.opt.gl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlFence;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import zako.opt.ZakoOptConfig;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class FrameFence implements GpuFence {
	private static Shared pending;

	Shared shared;
	@NonFinal
	boolean closed;

	public static boolean enabled() {
		return ZakoOptConfig.frameFence();
	}

	public static GpuFence create() {
		if (pending == null) {
			pending = new Shared();
		}
		pending.refs++;
		return new FrameFence(pending);
	}

	@SuppressWarnings("resource")
	public static void endFrame() {
		if (pending != null) {
			if (pending.refs > 0) {
				pending.real = new GlFence();
			}
			pending = null;
		}
	}

	@Override
	@SuppressWarnings("resource")
	public boolean awaitCompletion(long timeout) {
		// waited on before the frame ended: fence now, like vanilla would have
		if (shared.real == null) {
			shared.real = new GlFence();
			if (pending == shared) {
				pending = null;
			}
		}
		return shared.real.awaitCompletion(timeout);
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		if (--shared.refs == 0 && shared.real != null) {
			shared.real.close();
		}
	}

	@FieldDefaults(level = AccessLevel.PRIVATE)
	private static final class Shared {
		GlFence real;
		int refs;
	}
}
