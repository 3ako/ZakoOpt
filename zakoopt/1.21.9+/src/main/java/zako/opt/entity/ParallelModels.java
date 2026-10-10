package zako.opt.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import zako.opt.ZakoOptConfig;
import zako.opt.gl.RawVertices;
import zako.opt.mixin.entity.ModelPartAccessor;
import zako.opt.particle.Workers;

// Model vertices built on several threads. setupAnim writes the pose into a model instance shared by every entity
// of that kind, so the render thread poses each model and snapshots its parts, workers build the vertices from the
// snapshots while the next models are posed, and the render thread then hands them to the buffers in vanilla's order.
// Each version's ModelFeatureRenderer hook drives begin / add / end / emit.
@UtilityClass
public class ParallelModels {
	public final VertexFormat FORMAT = EntityFormat.FORMAT;
	public final int MIN_SUBMITS = 24;
	private final int STRIDE = FORMAT.getVertexSize();
	private final int FIELDS = 9;
	private final int PAGE = 1 << 20;

	private final Map<ModelPart, Flat> FLATS = new WeakHashMap<>();
	private final List<Job> BUILT = new ArrayList<>();
	private final ThreadLocal<PoseStack> STACKS = ThreadLocal.withInitial(PoseStack::new);
	private final ThreadLocal<float[]> CORNERS = ThreadLocal.withInitial(() -> new float[24]);
	// vertex memory handed out while models are still being posed, so the total is not known up front; reused every frame
	private final List<Long> PAGES = new ArrayList<>();
	private final List<Integer> PAGE_SIZES = new ArrayList<>();
	private int page;
	private int pageUsed;
	private Workers.Stream stream;

	public boolean enabled() {
		return ZakoOptConfig.parallelModels() && !ZakoOptConfig.entityAnimFreeze();
	}

	public void begin() {
		BUILT.clear();
		page = 0;
		pageUsed = 0;
		stream = Workers.stream(i -> build(BUILT.get(i)));
	}

	// render thread: poses the model and queues its vertices; the caller checks the vertex format and that nothing
	// else (outline, block cracks) has to see the same model
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Job add(Model model, Object state, PoseStack.Pose pose, int light, int overlay, int color, TextureAtlasSprite sprite) {
		model.setupAnim(state);
		Flat flat = FLATS.computeIfAbsent(model.root(), Flat::new);
		int n = flat.parts.length;
		Job job = new Job(flat, pose, light, overlay, Integer.reverseBytes(Integer.rotateLeft(color, 8)), sprite, new float[n * FIELDS], new byte[n]);
		int vertices = 0;
		for (int i = 0; i < n; i++) {
			ModelPart p = flat.parts[i];
			int o = i * FIELDS;
			job.pose[o] = p.x;
			job.pose[o + 1] = p.y;
			job.pose[o + 2] = p.z;
			job.pose[o + 3] = p.xRot;
			job.pose[o + 4] = p.yRot;
			job.pose[o + 5] = p.zRot;
			job.pose[o + 6] = p.xScale;
			job.pose[o + 7] = p.yScale;
			job.pose[o + 8] = p.zScale;
			job.flags[i] = (byte) ((p.visible ? 1 : 0) | (p.skipDraw ? 2 : 0));
		}
		for (int i = 0; i < n; ) {
			if ((job.flags[i] & 1) == 0) {
				i = flat.end[i];
				continue;
			}
			if ((job.flags[i] & 2) == 0) {
				vertices += flat.vertices[i];
			}
			i++;
		}
		job.vertices = vertices;
		job.address = reserve(vertices * STRIDE);
		BUILT.add(job);
		stream.publish(BUILT.size());
		return job;
	}

	// must run even when posing failed: helpers spin until the stream is finished
	public void end() {
		Throwable error = stream.finish(BUILT.size());
		stream = null;
		if (error != null) {
			throw new RuntimeException("zakoopt parallel models", error);
		}
	}

	// false: the consumer cannot take raw vertices, the caller renders this model the vanilla way
	public boolean emit(Job job, VertexConsumer consumer, MemoryStack stack) {
		if (job.vertices == 0) {
			return true;
		}
		if (!RawVertices.accepts(consumer, FORMAT)) {
			return false;
		}
		stack.push();
		try {
			RawVertices.push(consumer, stack, job.address, job.vertices, FORMAT);
		} finally {
			stack.pop();
		}
		return true;
	}

	private long reserve(int bytes) {
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
		poses.last().set(job.matrix);
		long[] ptr = {job.address};
		for (int i = 0; i < job.flat.parts.length; ) {
			i = part(job, poses, i, ptr);
		}
	}

	private int normal(float c) {
		return (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F) & 0xFF;
	}

	// mirrors ModelPart.render / translateAndRotate / Cube.compile, reading the snapshot instead of the shared part
	private int part(Job job, PoseStack poses, int i, long[] ptr) {
		Flat flat = job.flat;
		int end = flat.end[i];
		if ((job.flags[i] & 1) == 0 || flat.empty[i]) {
			return end;
		}
		float[] p = job.pose;
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
			PoseStack.Pose pose = poses.last();
			Matrix4f matrix = pose.pose();
			Vector3f v = new Vector3f();
			// what SpriteCoordinateExpander does to every vertex; the sprite's bounds are final, safe to read here
			TextureAtlasSprite sprite = job.sprite;
			for (Cube cube : flat.cubes[i]) {
				// a cube's 24 vertices share 8 corners: transform the corners once, as Sodium's cuboid renderer does
				float[] at = CORNERS.get();
				if (at.length < cube.corners.length) {
					at = new float[cube.corners.length];
					CORNERS.set(at);
				}
				for (int c = 0; c < cube.corners.length; c += 3) {
					matrix.transformPosition(cube.corners[c], cube.corners[c + 1], cube.corners[c + 2], v);
					at[c] = v.x();
					at[c + 1] = v.y();
					at[c + 2] = v.z();
				}
				int k = 0;
				for (int q = 0; q < cube.sizes.length; q++) {
					Vector3f n = pose.transformNormal(cube.normals[q * 3], cube.normals[q * 3 + 1], cube.normals[q * 3 + 2], v);
					int normal = normal(n.x()) | normal(n.y()) << 8 | normal(n.z()) << 16;
					for (int e = k + cube.sizes[q]; k < e; k++) {
						int c = cube.index[k] * 3;
						float u = cube.uv[k * 2], w = cube.uv[k * 2 + 1];
						if (sprite != null) {
							u = sprite.getU(u);
							w = sprite.getV(w);
						}
						long v0 = ptr[0];
						MemoryUtil.memPutFloat(v0, at[c]);
						MemoryUtil.memPutFloat(v0 + 4, at[c + 1]);
						MemoryUtil.memPutFloat(v0 + 8, at[c + 2]);
						MemoryUtil.memPutInt(v0 + 12, job.color);
						MemoryUtil.memPutFloat(v0 + 16, u);
						MemoryUtil.memPutFloat(v0 + 20, w);
						MemoryUtil.memPutInt(v0 + 24, job.overlay);
						MemoryUtil.memPutInt(v0 + 28, job.light);
						MemoryUtil.memPutInt(v0 + 32, normal);
						ptr[0] += STRIDE;
					}
				}
			}
		}
		for (int c = i + 1; c < end; ) {
			c = part(job, poses, c, ptr);
		}
		poses.popPose();
		return end;
	}

	public final class Job {
		final Flat flat;
		final PoseStack.Pose matrix;
		final int light;
		final int overlay;
		final int color;
		final TextureAtlasSprite sprite;
		final float[] pose;
		final byte[] flags;
		int vertices;
		long address;

		Job(Flat flat, PoseStack.Pose matrix, int light, int overlay, int color, TextureAtlasSprite sprite, float[] pose, byte[] flags) {
			this.flat = flat;
			this.matrix = matrix;
			this.light = light;
			this.overlay = overlay;
			this.color = color;
			this.sprite = sprite;
			this.pose = pose;
			this.flags = flags;
		}
	}

	// one cube in vanilla's polygon and vertex order, its vertices folded onto the distinct corners they sit on
	private final class Cube {
		final float[] corners;
		final int[] index;
		final float[] uv;
		final float[] normals;
		final int[] sizes;

		Cube(ModelPart.Cube cube) {
			List<Float> corner = new ArrayList<>();
			List<Integer> idx = new ArrayList<>();
			List<Float> uvs = new ArrayList<>();
			normals = new float[cube.polygons.length * 3];
			sizes = new int[cube.polygons.length];
			for (int q = 0; q < cube.polygons.length; q++) {
				ModelPart.Polygon polygon = cube.polygons[q];
				normals[q * 3] = polygon.normal().x();
				normals[q * 3 + 1] = polygon.normal().y();
				normals[q * 3 + 2] = polygon.normal().z();
				sizes[q] = polygon.vertices().length;
				for (ModelPart.Vertex vertex : polygon.vertices()) {
					int found = -1;
					for (int c = 0; c < corner.size(); c += 3) {
						if (corner.get(c) == vertex.worldX() && corner.get(c + 1) == vertex.worldY() && corner.get(c + 2) == vertex.worldZ()) {
							found = c / 3;
							break;
						}
					}
					if (found < 0) {
						found = corner.size() / 3;
						corner.add(vertex.worldX());
						corner.add(vertex.worldY());
						corner.add(vertex.worldZ());
					}
					idx.add(found);
					uvs.add(vertex.u());
					uvs.add(vertex.v());
				}
			}
			corners = new float[corner.size()];
			for (int c = 0; c < corners.length; c++) {
				corners[c] = corner.get(c);
			}
			index = idx.stream().mapToInt(Integer::intValue).toArray();
			uv = new float[uvs.size()];
			for (int c = 0; c < uv.length; c++) {
				uv[c] = uvs.get(c);
			}
		}
	}

	// the part tree in render order; end[i] is where the subtree of part i stops
	private final class Flat {
		final ModelPart[] parts;
		final int[] end;
		final int[] vertices;
		final boolean[] empty;
		final Cube[][] cubes;

		Flat(ModelPart root) {
			List<ModelPart> list = new ArrayList<>();
			List<Integer> ends = new ArrayList<>();
			add(root, list, ends);
			int n = list.size();
			parts = list.toArray(new ModelPart[0]);
			end = new int[n];
			vertices = new int[n];
			empty = new boolean[n];
			cubes = new Cube[n][];
			for (int i = 0; i < n; i++) {
				ModelPartAccessor acc = (ModelPartAccessor) (Object) parts[i];
				end[i] = ends.get(i);
				List<ModelPart.Cube> source = acc.zakoopt$cubes();
				cubes[i] = new Cube[source.size()];
				for (int c = 0; c < cubes[i].length; c++) {
					cubes[i][c] = new Cube(source.get(c));
					vertices[i] += cubes[i][c].index.length;
				}
				empty[i] = cubes[i].length == 0 && acc.zakoopt$children().isEmpty();
			}
		}

		private void add(ModelPart part, List<ModelPart> list, List<Integer> ends) {
			int at = list.size();
			list.add(part);
			ends.add(0);
			for (ModelPart child : ((ModelPartAccessor) (Object) part).zakoopt$children().values()) {
				add(child, list, ends);
			}
			ends.set(at, list.size());
		}
	}
}
