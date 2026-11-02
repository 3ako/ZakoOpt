package zako.opt.entity;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import zako.opt.gl.RawVertices;

import java.util.List;

// model cubes written straight into the buffer, as Sodium does it: a cube's 24 vertices share 8 corners, so the corners
// and the 6 face normals go through the matrices once instead of per vertex. Only runs without Sodium (VulkanMod, plain
// vanilla) and into a buffer that takes raw vertices; the bytes are the ones BufferBuilder's own fast path writes
@UtilityClass
public class FastCubes {
	public final VertexFormat FORMAT = DefaultVertexFormat.NEW_ENTITY;
	public final int STRIDE = 36;
	private final Scratch SCRATCH = new Scratch();
	private long buffer;
	private int capacity;

	public interface Holder {
		Baked zakoopt$baked();

		void zakoopt$baked(Baked baked);
	}

	public record Baked(float[] corners, int[] index, float[] uv, float[] normals, int[] faceSizes, int vertices) {
	}

	// per-thread working memory of write()
	public static final class Scratch {
		float[] corners = new float[24];
		final Vector3f tmp = new Vector3f();
	}

	// false: the consumer cannot take raw vertices, the caller draws the cubes the usual way
	public boolean render(List<ModelPart.Cube> cubes, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
		if (!RawVertices.accepts(consumer, FORMAT)) {
			return false;
		}
		int vertices = vertices(cubes);
		if (vertices == 0) {
			return true;
		}
		int bytes = vertices * STRIDE;
		if (bytes > capacity) {
			capacity = Math.max(bytes, capacity * 2);
			buffer = MemoryUtil.nmemRealloc(buffer, capacity);
		}
		write(cubes, pose, buffer, light, overlay, abgr(color), SCRATCH);
		RawVertices.push(consumer, MemoryStack.stackGet(), buffer, vertices, FORMAT);
		return true;
	}

	public int abgr(int color) {
		return color & 0xFF00FF00 | (color & 0xFF0000) >> 16 | (color & 0xFF) << 16;
	}

	// render thread: bakes cubes not seen before, so write() on other threads only reads
	public int vertices(List<ModelPart.Cube> cubes) {
		int vertices = 0;
		for (ModelPart.Cube cube : cubes) {
			vertices += baked(cube).vertices;
		}
		return vertices;
	}

	// returns the address after the last vertex written
	public long write(List<ModelPart.Cube> cubes, PoseStack.Pose pose, long ptr, int light, int overlay, int abgr, Scratch s) {
		Matrix4f matrix = pose.pose();
		Vector3f tmp = s.tmp;
		for (ModelPart.Cube cube : cubes) {
			Baked b = ((Holder) cube).zakoopt$baked();
			if (s.corners.length < b.corners.length) {
				s.corners = new float[b.corners.length];
			}
			float[] corners = s.corners;
			for (int c = 0; c < b.corners.length; c += 3) {
				matrix.transformPosition(b.corners[c], b.corners[c + 1], b.corners[c + 2], tmp);
				corners[c] = tmp.x;
				corners[c + 1] = tmp.y;
				corners[c + 2] = tmp.z;
			}
			int k = 0;
			for (int q = 0; q < b.faceSizes.length; q++) {
				pose.transformNormal(b.normals[q * 3], b.normals[q * 3 + 1], b.normals[q * 3 + 2], tmp);
				int normal = normal(tmp.x) | normal(tmp.y) << 8 | normal(tmp.z) << 16;
				for (int e = k + b.faceSizes[q]; k < e; k++) {
					int c = b.index[k] * 3;
					MemoryUtil.memPutFloat(ptr, corners[c]);
					MemoryUtil.memPutFloat(ptr + 4, corners[c + 1]);
					MemoryUtil.memPutFloat(ptr + 8, corners[c + 2]);
					MemoryUtil.memPutInt(ptr + 12, abgr);
					MemoryUtil.memPutFloat(ptr + 16, b.uv[k * 2]);
					MemoryUtil.memPutFloat(ptr + 20, b.uv[k * 2 + 1]);
					MemoryUtil.memPutInt(ptr + 24, overlay);
					MemoryUtil.memPutInt(ptr + 28, light);
					MemoryUtil.memPutInt(ptr + 32, normal);
					ptr += STRIDE;
				}
			}
		}
		return ptr;
	}

	private int normal(float c) {
		return (int) (Mth.clamp(c, -1.0F, 1.0F) * 127.0F) & 0xFF;
	}

	private Baked baked(ModelPart.Cube cube) {
		Holder holder = (Holder) cube;
		Baked baked = holder.zakoopt$baked();
		if (baked == null) {
			baked = bake(cube);
			holder.zakoopt$baked(baked);
		}
		return baked;
	}

	private Baked bake(ModelPart.Cube cube) {
		ModelPart.Polygon[] polygons = cube.polygons;
		int vertices = 0;
		for (ModelPart.Polygon p : polygons) {
			vertices += p.vertices().length;
		}
		float[] unique = new float[vertices * 3];
		int uniqueCount = 0;
		int[] index = new int[vertices];
		float[] uv = new float[vertices * 2];
		float[] normals = new float[polygons.length * 3];
		int[] faceSizes = new int[polygons.length];
		int k = 0;
		for (int q = 0; q < polygons.length; q++) {
			ModelPart.Polygon p = polygons[q];
			normals[q * 3] = p.normal().x();
			normals[q * 3 + 1] = p.normal().y();
			normals[q * 3 + 2] = p.normal().z();
			faceSizes[q] = p.vertices().length;
			for (ModelPart.Vertex v : p.vertices()) {
				// the same division vanilla does per vertex, so the corner floats match exactly
				float x = v.pos().x() / 16.0F, y = v.pos().y() / 16.0F, z = v.pos().z() / 16.0F;
				int found = -1;
				for (int c = 0; c < uniqueCount && found < 0; c++) {
					if (unique[c * 3] == x && unique[c * 3 + 1] == y && unique[c * 3 + 2] == z) {
						found = c;
					}
				}
				if (found < 0) {
					found = uniqueCount++;
					unique[found * 3] = x;
					unique[found * 3 + 1] = y;
					unique[found * 3 + 2] = z;
				}
				index[k] = found;
				uv[k * 2] = v.u();
				uv[k * 2 + 1] = v.v();
				k++;
			}
		}
		float[] corners = new float[uniqueCount * 3];
		System.arraycopy(unique, 0, corners, 0, corners.length);
		return new Baked(corners, index, uv, normals, faceSizes, vertices);
	}
}
