package zako.opt.gl;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.util.color.FastCubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

// Sodium's FastCubicSampler with the same weights, but each distinct colour is transformed and weighed once: across a biome
// border Sodium made four vectors for each of the 216 samples, for the sky and the fog every frame. Render thread only
@UtilityClass
public class CubicColors {
	private final double[] CURVE = {0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};
	private final int[] VALUES = new int[216];
	private final int[] COLORS = new int[216];
	private final double[] WEIGHTS = new double[216];

	public Vec3 sample(Vec3 pos, FastCubicSampler.ColorFetcher fetcher, Function<Vec3, Vec3> transformer) {
		int ix = Mth.floor(pos.x), iy = Mth.floor(pos.y), iz = Mth.floor(pos.z);
		int[] values = VALUES;
		boolean same = true;
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				for (int z = 0; z < 6; z++) {
					int v = values[36 * z + 6 * y + x] = fetcher.fetch(ix - 2 + x, iy - 2 + y, iz - 2 + z);
					same &= v == values[0];
				}
			}
		}
		if (same) {
			return transformer.apply(Vec3.fromRGB24(values[0]));
		}
		double dx = pos.x - ix, dy = pos.y - iy, dz = pos.z - iz;
		int n = 0;
		double total = 0.0;
		for (int x = 0; x < 6; x++) {
			double wx = Mth.lerp(dx, CURVE[x + 1], CURVE[x]);
			for (int y = 0; y < 6; y++) {
				double wy = Mth.lerp(dy, CURVE[y + 1], CURVE[y]);
				for (int z = 0; z < 6; z++) {
					double factor = wx * wy * Mth.lerp(dz, CURVE[z + 1], CURVE[z]);
					total += factor;
					int v = values[36 * z + 6 * y + x];
					int k = 0;
					while (k < n && COLORS[k] != v) {
						k++;
					}
					if (k == n) {
						COLORS[n] = v;
						WEIGHTS[n++] = 0.0;
					}
					WEIGHTS[k] += factor;
				}
			}
		}
		double r = 0.0, g = 0.0, b = 0.0;
		for (int k = 0; k < n; k++) {
			Vec3 c = transformer.apply(Vec3.fromRGB24(COLORS[k]));
			r += c.x * WEIGHTS[k];
			g += c.y * WEIGHTS[k];
			b += c.z * WEIGHTS[k];
		}
		double scale = 1.0 / total;
		return new Vec3(r * scale, g * scale, b * scale);
	}
}
