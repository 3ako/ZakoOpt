package zako.opt.entity;

import lombok.experimental.UtilityClass;
import org.lwjgl.system.MemoryStack;

// MemoryStack.stackPush() without the ThreadLocal lookup for the thread that called last (the render thread)
@UtilityClass
public class CachedStack {
	private record ThreadStack(Thread thread, MemoryStack stack) {
	}

	// one record so another thread never sees a thread/stack pair half-updated
	private ThreadStack cached = new ThreadStack(null, null);

	public MemoryStack push() {
		ThreadStack current = cached;
		Thread thread = Thread.currentThread();
		if (current.thread != thread) {
			current = new ThreadStack(thread, MemoryStack.stackGet());
			cached = current;
		}
		return current.stack.push();
	}
}
