package zako.opt.particle;

import lombok.experimental.UtilityClass;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

@UtilityClass
public class Workers {
	final int COUNT = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 2));
	private final AtomicInteger THREAD_ID = new AtomicInteger();
	private final ExecutorService POOL = Executors.newFixedThreadPool(COUNT, r -> {
		Thread t = new Thread(r, "zakoopt-worker-" + THREAD_ID.incrementAndGet());
		t.setDaemon(true);
		return t;
	});

	// runs task(0..blocks-1) on helpers and the calling thread; returns the first failure, null if none
	public Throwable run(int blocks, IntConsumer task) {
		Job job = new Job(blocks, task);
		for (int h = Math.min(COUNT, blocks - 1); h > 0; h--) {
			POOL.execute(job::run);
		}
		// the caller works too; it only ever waits for blocks a helper has already claimed
		job.run();
		while (job.completed.get() < blocks) {
			Thread.onSpinWait();
		}
		return job.error.get();
	}

	// helpers start right away and take items as the caller publishes them, so work overlaps with preparing the rest
	public Stream stream(IntConsumer task) {
		Stream stream = new Stream(task);
		for (int h = COUNT; h > 0; h--) {
			POOL.execute(stream::work);
		}
		return stream;
	}

	public final class Stream {
		final IntConsumer task;
		final AtomicInteger next = new AtomicInteger();
		final AtomicInteger completed = new AtomicInteger();
		final AtomicReference<Throwable> error = new AtomicReference<>();
		volatile int ready;
		volatile int total = -1;

		Stream(IntConsumer task) {
			this.task = task;
		}

		// items 0..count-1 are fully set up and may be taken
		public void publish(int count) {
			ready = count;
		}

		// the caller helps with what is left and waits for the rest; returns the first failure, null if none
		public Throwable finish(int count) {
			ready = count;
			total = count;
			work();
			while (completed.get() < count) {
				Thread.onSpinWait();
			}
			return error.get();
		}

		void work() {
			while (true) {
				int n = next.get();
				if (n < ready) {
					if (next.compareAndSet(n, n + 1)) {
						try {
							if (error.get() == null) {
								task.accept(n);
							}
						} catch (Throwable t) {
							error.compareAndSet(null, t);
						} finally {
							completed.incrementAndGet();
						}
					}
					continue;
				}
				int t = total;
				if (t >= 0 && n >= t) {
					return;
				}
				Thread.onSpinWait();
			}
		}
	}

	private final class Job {
		final int blocks;
		final IntConsumer task;
		final AtomicInteger next = new AtomicInteger();
		final AtomicInteger completed = new AtomicInteger();
		final AtomicReference<Throwable> error = new AtomicReference<>();

		Job(int blocks, IntConsumer task) {
			this.blocks = blocks;
			this.task = task;
		}

		void run() {
			int b;
			while ((b = next.getAndIncrement()) < blocks) {
				try {
					if (error.get() == null) {
						task.accept(b);
					}
				} catch (Throwable t) {
					error.compareAndSet(null, t);
				} finally {
					completed.incrementAndGet();
				}
			}
		}
	}
}
