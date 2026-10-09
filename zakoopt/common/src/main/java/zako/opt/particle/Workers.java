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
	Throwable run(int blocks, IntConsumer task) {
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
