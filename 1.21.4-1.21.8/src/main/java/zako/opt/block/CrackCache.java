package zako.opt.block;

import zako.opt.feature.Features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// block cracks: each breaking block was tessellated again every frame (model, AO, chunk and light reads) and switched the
// immediate crumbling buffer, an upload and a draw per block. Here a block's crack mesh is built once per state and stage,
// in block space, and a frame's cracks go out sorted by stage: one draw per stage
@UtilityClass
public class CrackCache {
	// x y z u v light nx ny nz per vertex, light as float bits
	private final int STRIDE = 9;
	private final int STAGES = ModelBakery.DESTROY_TYPES.size();
	private final Long2ObjectOpenHashMap<Mesh> MESHES = new Long2ObjectOpenHashMap<>();
	@SuppressWarnings("unchecked")
	private final List<Draw>[] QUEUED = new List[STAGES];
	private final List<Draw> POOL = new ArrayList<>();
	private final Recorder RECORDER = new Recorder();
	private final PoseStack ORIGIN = new PoseStack();
	private final VertexConsumer DECAL = new SheetedDecalTextureGenerator(RECORDER, ORIGIN.last(), 1.0F);
	// what the vanilla loop's own getBuffer hands out while cracks are queued: nothing is drawn through it
	public final VertexConsumer IDLE = new Idle();
	private final Vector3f NORMAL = new Vector3f();
	private long frame;
	private int pooled;
	// set once the tessellation hook has run: until then the crumbling buffer is left to vanilla, so a hook that never
	// applies cannot leave the cracks invisible
	public boolean hooked;

	static {
		for (int i = 0; i < STAGES; i++) {
			QUEUED[i] = new ArrayList<>();
		}
	}

	@FieldDefaults(level = AccessLevel.PRIVATE)
	private final class Mesh {
		BlockState state;
		int stage;
		float[] data;
		int vertices;
		long frame;
	}

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	private final class Draw {
		Matrix4f pose = new Matrix4f();
		Matrix3f normal = new Matrix3f();
		@NonFinal
		Mesh mesh;
	}

	public boolean enabled() {
		return Features.CRACK_CACHE.on;
	}

	// true: the block's mesh must be built again, by drawing it into recorder() with origin() as its pose, then built()
	public boolean stale(long pos, BlockState state, int stage) {
		hooked = true;
		Mesh mesh = MESHES.get(pos);
		if (mesh == null) {
			mesh = new Mesh();
			MESHES.put(pos, mesh);
		}
		mesh.frame = frame;
		if (mesh.data != null && mesh.state == state && mesh.stage == stage) {
			return false;
		}
		mesh.state = state;
		mesh.stage = stage;
		RECORDER.vertices = 0;
		return true;
	}

	public PoseStack origin() {
		return ORIGIN;
	}

	// the decal generator the vanilla call would have drawn through, writing into the recorder in block space
	public VertexConsumer recorder() {
		return DECAL;
	}

	public void built(long pos) {
		Mesh mesh = MESHES.get(pos);
		mesh.vertices = RECORDER.vertices;
		mesh.data = Arrays.copyOf(RECORDER.data, RECORDER.vertices * STRIDE);
	}

	// stands in for the crack's own drawing at this pose; the stage comes with the mesh
	public void queue(long pos, PoseStack poseStack) {
		Mesh mesh = MESHES.get(pos);
		if (mesh.vertices == 0 || mesh.stage < 0 || mesh.stage >= STAGES) {
			return;
		}
		if (pooled == POOL.size()) {
			POOL.add(new Draw());
		}
		Draw draw = POOL.get(pooled++);
		draw.pose.set(poseStack.last().pose());
		draw.normal.set(poseStack.last().normal());
		draw.mesh = mesh;
		QUEUED[mesh.stage].add(draw);
	}

	// after the crack loop: every queued crack, one getBuffer per stage; the last batch stays open for vanilla's own flush
	public void flush(MultiBufferSource.BufferSource crumbling) {
		for (int stage = 0; stage < STAGES; stage++) {
			List<Draw> draws = QUEUED[stage];
			if (draws.isEmpty()) {
				continue;
			}
			VertexConsumer out = crumbling.getBuffer(ModelBakery.DESTROY_TYPES.get(stage));
			for (Draw draw : draws) {
				float[] d = draw.mesh.data;
				for (int v = 0, o = 0; v < draw.mesh.vertices; v++, o += STRIDE) {
					draw.normal.transform(d[o + 6], d[o + 7], d[o + 8], NORMAL);
					out.addVertex(draw.pose, d[o], d[o + 1], d[o + 2]).setColor(-1).setUv(d[o + 3], d[o + 4])
							.setLight(Float.floatToRawIntBits(d[o + 5])).setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
				}
				draw.mesh = null;
			}
			draws.clear();
		}
		pooled = 0;
		// blocks no longer breaking (or out of range) drop their mesh
		MESHES.values().removeIf(m -> m.frame != frame);
		frame++;
	}

	public void clear() {
		MESHES.clear();
	}

	private final class Recorder implements VertexConsumer {
		float[] data = new float[STRIDE * 64];
		int vertices;

		private int at() {
			return (vertices - 1) * STRIDE;
		}

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			if ((vertices + 1) * STRIDE > data.length) {
				data = Arrays.copyOf(data, data.length * 2);
			}
			vertices++;
			int o = at();
			data[o] = x;
			data[o + 1] = y;
			data[o + 2] = z;
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			int o = at();
			data[o + 3] = u;
			data[o + 4] = v;
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			data[at() + 5] = Float.intBitsToFloat(u & 0xFFFF | v << 16);
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			int o = at();
			data[o + 6] = x;
			data[o + 7] = y;
			data[o + 8] = z;
			return this;
		}
	}

	private final class Idle implements VertexConsumer {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int r, int g, int b, int a) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	}
}
