package zako.opt.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryUtil;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.ReserveBuffer;
import zako.opt.mixin.entity.ModelPartAccessor;
import zako.opt.mixin.entity.PoseAccessor;
import zako.opt.particle.Workers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

// 1.21.4 renders every model straight into its buffer. During the entity pass the render thread only snapshots the posed
// parts (setupAnim writes them into a model instance shared by every entity of that kind) and reserves the model's
// vertices in the buffer, so vanilla's order holds; workers write the vertices into staging memory, which is copied into
// the reserved range before that buffer is built and at the end of the pass. FastCubes and the pose maths here are
// vanilla's byte for byte, so with Sodium the models come out as vanilla draws them, not as Sodium's own writer would
@UtilityClass
public class ParallelModels {
	private final int FIELDS = 9;
	private final int PAGE = 1 << 20;

	private final Map<ModelPart, Flat> FLATS = new WeakHashMap<>();
	private final List<Job> JOBS = new ArrayList<>();
	private final Map<ReserveBuffer, List<Job>> PENDING = new IdentityHashMap<>();
	private final ThreadLocal<PoseStack> STACKS = ThreadLocal.withInitial(PoseStack::new);
	private final ThreadLocal<FastCubes.Scratch> SCRATCH = ThreadLocal.withInitial(FastCubes.Scratch::new);
	private final List<Long> PAGES = new ArrayList<>();
	private final List<Integer> PAGE_SIZES = new ArrayList<>();
	private int page;
	private int pageUsed;
	private Workers.Stream stream;

	// a model tree flattened in render order; end[i] is the index after part i's subtree
	private final class Flat {
		final ModelPart[] parts;
		final int[] end;
		final boolean[] empty;
		final List<List<ModelPart.Cube>> cubes = new ArrayList<>();

		Flat(ModelPart root) {
			List<ModelPart> list = new ArrayList<>();
			List<Integer> ends = new ArrayList<>();
			collect(root, list, ends);
			parts = list.toArray(new ModelPart[0]);
			end = ends.stream().mapToInt(Integer::intValue).toArray();
			empty = new boolean[parts.length];
			for (int i = 0; i < parts.length; i++) {
				ModelPartAccessor p = (ModelPartAccessor) (Object) parts[i];
				cubes.add(p.zakoopt$cubes());
				empty[i] = p.zakoopt$cubes().isEmpty() && p.zakoopt$children().isEmpty();
			}
		}

		private void collect(ModelPart part, List<ModelPart> list, List<Integer> ends) {
			int i = list.size();
			list.add(part);
			ends.add(0);
			for (ModelPart child : ((ModelPartAccessor) (Object) part).zakoopt$children().values()) {
				collect(child, list, ends);
			}
			ends.set(i, list.size());
		}
	}

	private final class Job {
		final Flat flat;
		final PoseStack.Pose pose;
		final boolean trustedNormals;
		final int light, overlay, abgr;
		final float[] parts;
		final byte[] flags;
		long staging;
		int bytes;
		long offset;

		Job(Flat flat, PoseStack.Pose pose, int light, int overlay, int abgr) {
			this.flat = flat;
			this.pose = pose;
			this.trustedNormals = ((PoseAccessor) (Object) pose).zakoopt$trustedNormals();
			this.light = light;
			this.overlay = overlay;
			this.abgr = abgr;
			this.parts = new float[flat.parts.length * FIELDS];
			this.flags = new byte[flat.parts.length];
		}
	}

	public boolean enabled() {
		return ZakoOptConfig.parallelModels() && ZakoOptConfig.fastCubes();
	}

	public void begin() {
		if (!enabled()) {
			return;
		}
		JOBS.clear();
		page = 0;
		pageUsed = 0;
		stream = Workers.stream(i -> build(JOBS.get(i)));
	}

	// must run even when the pass failed: helpers spin until the stream is finished
	public void end() {
		if (stream == null) {
			return;
		}
		Throwable error;
		try {
			error = stream.finish(JOBS.size());
			PENDING.forEach((buffer, jobs) -> copy(buffer, jobs));
		} finally {
			stream = null;
			PENDING.clear();
			JOBS.clear();
		}
		if (error != null) {
			throw new RuntimeException("zakoopt parallel models", error);
		}
	}

	// render thread, from Model.renderToBuffer; false: render the model the usual way
	public boolean add(Model model, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
		if (stream == null || !(consumer instanceof ReserveBuffer buffer) || !buffer.zakoopt$accepts(FastCubes.FORMAT) || !RenderSystem.isOnRenderThread()) {
			return false;
		}
		Flat flat = FLATS.computeIfAbsent(model.root(), Flat::new);
		Job job = new Job(flat, poseStack.last().copy(), light, overlay, FastCubes.abgr(color));
		int vertices = 0;
		for (int i = 0; i < flat.parts.length; i++) {
			ModelPart p = flat.parts[i];
			int o = i * FIELDS;
			job.parts[o] = p.x;
			job.parts[o + 1] = p.y;
			job.parts[o + 2] = p.z;
			job.parts[o + 3] = p.xRot;
			job.parts[o + 4] = p.yRot;
			job.parts[o + 5] = p.zRot;
			job.parts[o + 6] = p.xScale;
			job.parts[o + 7] = p.yScale;
			job.parts[o + 8] = p.zScale;
			job.flags[i] = (byte) ((p.visible ? 1 : 0) | (p.skipDraw ? 2 : 0));
		}
		for (int i = 0; i < flat.parts.length; ) {
			if ((job.flags[i] & 1) == 0 || flat.empty[i]) {
				i = flat.end[i];
				continue;
			}
			if ((job.flags[i] & 2) == 0) {
				vertices += FastCubes.vertices(flat.cubes.get(i));
			}
			i++;
		}
		if (vertices == 0) {
			return true;
		}
		job.bytes = vertices * FastCubes.STRIDE;
		job.staging = staging(job.bytes);
		job.offset = buffer.zakoopt$reserve(vertices);
		PENDING.computeIfAbsent(buffer, b -> new ArrayList<>()).add(job);
		JOBS.add(job);
		stream.publish(JOBS.size());
		return true;
	}

	// render thread, before a buffer is built: its reserved vertices have to be there
	public void sync(ReserveBuffer buffer) {
		List<Job> jobs = PENDING.remove(buffer);
		if (jobs == null) {
			return;
		}
		Throwable error = stream.await(JOBS.size());
		if (error != null) {
			throw new RuntimeException("zakoopt parallel models", error);
		}
		copy(buffer, jobs);
	}

	private void copy(ReserveBuffer buffer, List<Job> jobs) {
		for (Job job : jobs) {
			MemoryUtil.memCopy(job.staging, buffer.zakoopt$address(job.offset), job.bytes);
		}
	}

	private long staging(int bytes) {
		if (page < PAGES.size() && pageUsed + bytes > PAGE_SIZES.get(page)) {
			page++;
			pageUsed = 0;
		}
		if (page == PAGES.size()) {
			int size = Math.max(PAGE, bytes);
			PAGES.add(MemoryUtil.nmemAlloc(size));
			PAGE_SIZES.add(size);
		} else if (bytes > PAGE_SIZES.get(page)) {
			MemoryUtil.nmemFree(PAGES.get(page));
			PAGES.set(page, MemoryUtil.nmemAlloc(bytes));
			PAGE_SIZES.set(page, bytes);
		}
		long address = PAGES.get(page) + pageUsed;
		pageUsed += bytes;
		return address;
	}

	private void build(Job job) {
		PoseStack poses = STACKS.get();
		PoseStack.Pose base = poses.last();
		base.pose().set(job.pose.pose());
		base.normal().set(job.pose.normal());
		((PoseAccessor) (Object) base).zakoopt$trustedNormals(job.trustedNormals);
		long[] ptr = {job.staging};
		FastCubes.Scratch scratch = SCRATCH.get();
		for (int i = 0; i < job.flat.parts.length; ) {
			i = part(job, poses, i, ptr, scratch);
		}
	}

	// mirrors ModelPart.render / translateAndRotate, reading the snapshot instead of the shared part
	private int part(Job job, PoseStack poses, int i, long[] ptr, FastCubes.Scratch scratch) {
		Flat flat = job.flat;
		int end = flat.end[i];
		if ((job.flags[i] & 1) == 0 || flat.empty[i]) {
			return end;
		}
		float[] p = job.parts;
		int o = i * FIELDS;
		poses.pushPose();
		poses.translate(p[o] / 16.0F, p[o + 1] / 16.0F, p[o + 2] / 16.0F);
		if (p[o + 3] != 0.0F || p[o + 4] != 0.0F || p[o + 5] != 0.0F) {
			poses.mulPose(new Quaternionf().rotationZYX(p[o + 5], p[o + 4], p[o + 3]));
		}
		if (p[o + 6] != 1.0F || p[o + 7] != 1.0F || p[o + 8] != 1.0F) {
			poses.scale(p[o + 6], p[o + 7], p[o + 8]);
		}
		if ((job.flags[i] & 2) == 0) {
			ptr[0] = FastCubes.write(flat.cubes.get(i), poses.last(), ptr[0], job.light, job.overlay, job.abgr, scratch);
		}
		for (int c = i + 1; c < end; ) {
			c = part(job, poses, c, ptr, scratch);
		}
		poses.popPose();
		return end;
	}
}
