package zako.opt.gl;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;

import java.util.Arrays;

// prototype: a depth map drawn on the CPU from the tops of solid opaque ground, for the same section test the GPU depth copy feeds
@UtilityClass
public class CpuOcclusion {
	private final int GROUP = 4;
	private final int RADIUS = 96;
	private final int SCAN = 64;
	private final long STALE_NS = 4_000_000_000L;
	private final int REFRESH_PER_FRAME = 8;

	private record Columns(short[] top, byte[] solid, long builtNs) {
	}

	private final Long2ObjectOpenHashMap<Columns> chunks = new Long2ObjectOpenHashMap<>();
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final float[] clip = new float[4 * 8], poly = new float[4 * 8], screen = new float[3 * 8];
	public int width, height;
	public float[] depth = new float[0];
	private float far;
	private boolean reversed, zeroToOne;
	private int refreshed;
	public int groups, planes, covered, missing;

	public void build(Matrix4f m, double camX, double camY, double camZ, boolean reversed, boolean zeroToOne, float aspect) {
		var level = Minecraft.getInstance().level;
		CpuOcclusion.reversed = reversed;
		CpuOcclusion.zeroToOne = zeroToOne;
		width = 256;
		height = Math.max(16, Math.round(256 / aspect));
		if (depth.length != width * height) {
			depth = new float[width * height];
		}
		far = reversed ? 0f : 1f;
		Arrays.fill(depth, far);
		if (level == null) {
			return;
		}
		refreshed = 0;
		groups = planes = covered = missing = 0;
		int bx = (int) Math.floor(camX) & ~(GROUP - 1), bz = (int) Math.floor(camZ) & ~(GROUP - 1);
		for (int gx = bx - RADIUS; gx < bx + RADIUS; gx += GROUP) {
			for (int gz = bz - RADIUS; gz < bz + RADIUS; gz += GROUP) {
				int plane = plane(level, gx, gz);
				groups++;
				if (plane != Integer.MIN_VALUE) {
					planes++;
					quad(m, (float) (gx - camX), (float) (plane - camY), (float) (gz - camZ), GROUP);
				}
			}
		}
		erode();
	}

	// centre sampling lets neighbouring planes cover a texel together; taking the farthest of each 3x3 afterwards gives back
	// the margin, so a texel only hides what lies behind occluders on all sides of it
	private float[] scratch = new float[0];

	private void erode() {
		if (scratch.length != depth.length) {
			scratch = new float[depth.length];
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float v = depth[y * width + x];
				for (int dy = -1; dy <= 1; dy++) {
					int yy = y + dy;
					for (int dx = -1; dx <= 1; dx++) {
						int xx = x + dx;
						float n = xx < 0 || yy < 0 || xx >= width || yy >= height ? far : depth[yy * width + xx];
						v = reversed ? Math.min(v, n) : Math.max(v, n);
					}
				}
				scratch[y * width + x] = v;
			}
		}
		float[] t = depth;
		depth = scratch;
		scratch = t;
	}

	// the height of a horizontal face that is opaque over the whole group, or MIN_VALUE when there is none
	private int plane(net.minecraft.client.multiplayer.ClientLevel level, int gx, int gz) {
		Columns c = columns(level, gx >> 4, gz >> 4);
		if (c == null) {
			missing++;
			return Integer.MIN_VALUE;
		}
		int lx = gx & 15, lz = gz & 15, low = Integer.MAX_VALUE, deepest = Integer.MIN_VALUE;
		for (int x = lx; x < lx + GROUP; x++) {
			for (int z = lz; z < lz + GROUP; z++) {
				int i = z << 4 | x, top = c.top[i];
				if (top == Short.MIN_VALUE) {
					return Integer.MIN_VALUE;
				}
				low = Math.min(low, top);
				deepest = Math.max(deepest, top - c.solid[i] + 1);
			}
		}
		// every column is solid at the lowest top, so a face there lies inside opaque blocks everywhere
		return deepest <= low ? low + 1 : Integer.MIN_VALUE;
	}

	private Columns columns(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz) {
		long key = cx & 0xFFFFFFFFL | (cz & 0xFFFFFFFFL) << 32;
		Columns c = chunks.get(key);
		long now = System.nanoTime();
		if (c != null && (now - c.builtNs < STALE_NS || refreshed >= REFRESH_PER_FRAME)) {
			return c;
		}
		if (!(level.getChunkSource().getChunk(cx, cz, false) instanceof LevelChunk chunk)) {
			return null;
		}
		refreshed++;
		short[] top = new short[256];
		byte[] solid = new byte[256];
		int minY = level.getMinBuildHeight();
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), found = Short.MIN_VALUE, stop = Math.max(minY, y - SCAN);
				for (; y >= stop; y--) {
					if (chunk.getBlockState(pos.set(x, y, z)).isSolidRender(chunk, pos)) {
						found = y;
						break;
					}
				}
				int run = 0;
				if (found != Short.MIN_VALUE) {
					for (int d = found; d > found - 16 && d >= minY && chunk.getBlockState(pos.set(x, d, z)).isSolidRender(chunk, pos); d--) {
						run++;
					}
				}
				top[z << 4 | x] = (short) found;
				solid[z << 4 | x] = (byte) run;
			}
		}
		c = new Columns(top, solid, now);
		chunks.put(key, c);
		if (chunks.size() > 4096) {
			chunks.clear();
		}
		return c;
	}

	private void quad(Matrix4f m, float x, float y, float z, float size) {
		float[] corners = {x, z, x + size, z, x + size, z + size, x, z + size};
		for (int i = 0; i < 4; i++) {
			float px = corners[i * 2], pz = corners[i * 2 + 1];
			clip[i * 4] = m.m00() * px + m.m10() * y + m.m20() * pz + m.m30();
			clip[i * 4 + 1] = m.m01() * px + m.m11() * y + m.m21() * pz + m.m31();
			clip[i * 4 + 2] = m.m02() * px + m.m12() * y + m.m22() * pz + m.m32();
			clip[i * 4 + 3] = m.m03() * px + m.m13() * y + m.m23() * pz + m.m33();
		}
		// clipped against w > NEAR_W: the ground under the camera is the occluder that matters most and always crosses it
		int n = clipNear(4);
		if (n < 3) {
			return;
		}
		for (int i = 0; i < n; i++) {
			float w = poly[i * 4 + 3];
			float nz = poly[i * 4 + 2] / w;
			screen[i * 3] = (poly[i * 4] / w * 0.5f + 0.5f) * width;
			screen[i * 3 + 1] = (poly[i * 4 + 1] / w * 0.5f + 0.5f) * height;
			screen[i * 3 + 2] = zeroToOne ? nz : nz * 0.5f + 0.5f;
		}
		for (int i = 1; i + 1 < n; i++) {
			triangle(0, i, i + 1);
		}
	}

	private int clipNear(int n) {
		final float near = 0.05f;
		int out = 0;
		for (int i = 0; i < n; i++) {
			int a = i * 4, b = ((i + 1) % n) * 4;
			float wa = clip[a + 3], wb = clip[b + 3];
			boolean ina = wa > near, inb = wb > near;
			if (ina) {
				System.arraycopy(clip, a, poly, out++ * 4, 4);
			}
			if (ina != inb) {
				float t = (near - wa) / (wb - wa);
				for (int k = 0; k < 4; k++) {
					poly[out * 4 + k] = clip[a + k] + (clip[b + k] - clip[a + k]) * t;
				}
				out++;
			}
		}
		return out;
	}

	// a texel is covered when its centre is, at the triangle's farthest depth over the texel
	private void triangle(int a, int b, int c) {
		float x0 = screen[a * 3], y0 = screen[a * 3 + 1], z0 = screen[a * 3 + 2];
		float x1 = screen[b * 3], y1 = screen[b * 3 + 1], z1 = screen[b * 3 + 2];
		float x2 = screen[c * 3], y2 = screen[c * 3 + 1], z2 = screen[c * 3 + 2];
		float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
		if (Math.abs(area) < 1e-4f) {
			return;
		}
		if (area < 0) {
			float t = x1; x1 = x2; x2 = t;
			t = y1; y1 = y2; y2 = t;
			t = z1; z1 = z2; z2 = t;
			area = -area;
		}
		// depth plane z = za + dzdx (x - x0) + dzdy (y - y0)
		float dzdx = ((z1 - z0) * (y2 - y0) - (z2 - z0) * (y1 - y0)) / area;
		float dzdy = ((z2 - z0) * (x1 - x0) - (z1 - z0) * (x2 - x0)) / area;
		float slack = (Math.abs(dzdx) + Math.abs(dzdy)) * 0.5f;
		int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
		int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
		int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
		int maxY = Math.min(height - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
		// edge e(x, y) = A x + B y + C, positive inside
		float a0 = y1 - y2, b0 = x2 - x1, c0 = x1 * y2 - x2 * y1;
		float a1 = y2 - y0, b1 = x0 - x2, c1 = x2 * y0 - x0 * y2;
		float a2 = y0 - y1, b2 = x1 - x0, c2 = x0 * y1 - x1 * y0;
		for (int py = minY; py <= maxY; py++) {
			float cy = py + 0.5f;
			int row = py * width;
			for (int px = minX; px <= maxX; px++) {
				float cx = px + 0.5f;
				if (a0 * cx + b0 * cy + c0 < 0 || a1 * cx + b1 * cy + c1 < 0 || a2 * cx + b2 * cy + c2 < 0) {
					continue;
				}
				float zc = z0 + dzdx * (cx - x0) + dzdy * (cy - y0);
				float d = reversed ? zc - slack : zc + slack;
				int i = row + px;
				covered++;
				if (reversed ? d > depth[i] : d < depth[i]) {
					depth[i] = d;
				}
			}
		}
	}
}
