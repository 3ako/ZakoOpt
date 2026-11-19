package zako.opt.render;

import zako.opt.feature.Features;
import zako.opt.particle.Workers;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import zako.opt.mixin.render.WeatherEffectRendererInvoker;

import java.util.ArrayList;
import java.util.List;

// extractRenderState walks (2r+1)^2 columns every frame and each one asks the heightmap, the biome and the light engine.
// The columns are independent reads, so rows go to the workers and the finished lists are merged in vanilla's order.
@UtilityClass
public class ParallelWeather {
	public boolean extract(WeatherEffectRenderer renderer, Level level, int ticks, float partialTick, Vec3 camera, WeatherRenderState state) {
		if (!Features.PARALLEL_WEATHER.on || !Minecraft.getInstance().isSameThread()) {
			return false;
		}
		state.intensity = level.getRainLevel(partialTick);
		if (state.intensity <= 0.0F) {
			return true;
		}
		state.radius = Minecraft.getInstance().options.weatherRadius().get();
		int radius = state.radius;
		int rows = radius * 2 + 1;
		if (rows <= 1) {
			return false;
		}
		int centerX = Mth.floor(camera.x);
		int centerY = Mth.floor(camera.y);
		int centerZ = Mth.floor(camera.z);
		@SuppressWarnings("unchecked")
		List<WeatherEffectRenderer.ColumnInstance>[] rain = new List[rows];
		@SuppressWarnings("unchecked")
		List<WeatherEffectRenderer.ColumnInstance>[] snow = new List[rows];
		WeatherEffectRendererInvoker invoker = (WeatherEffectRendererInvoker) renderer;
		Throwable error = Workers.run(rows, row -> {
			int z = centerZ - radius + row;
			List<WeatherEffectRenderer.ColumnInstance> rainRow = new ArrayList<>();
			List<WeatherEffectRenderer.ColumnInstance> snowRow = new ArrayList<>();
			RandomSource random = RandomSource.create();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = centerX - radius; x <= centerX + radius; x++) {
				int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				int bottom = Math.max(centerY - radius, height);
				int top = Math.max(centerY + radius, height);
				if (top - bottom == 0) {
					continue;
				}
				Biome.Precipitation precipitation = invoker.zakoopt$precipitation(level, pos.set(x, centerY, z));
				if (precipitation == Biome.Precipitation.NONE) {
					continue;
				}
				random.setSeed(x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761);
				int light = LevelRenderer.getLightColor(level, pos.set(x, Math.max(centerY, height), z));
				if (precipitation == Biome.Precipitation.RAIN) {
					rainRow.add(invoker.zakoopt$rain(random, ticks, x, bottom, top, z, light, partialTick));
				} else if (precipitation == Biome.Precipitation.SNOW) {
					snowRow.add(invoker.zakoopt$snow(random, ticks, x, bottom, top, z, light, partialTick));
				}
			}
			rain[row] = rainRow;
			snow[row] = snowRow;
		});
		if (error != null) {
			// vanilla runs the whole loop again; nothing was added to the state
			return false;
		}
		for (int row = 0; row < rows; row++) {
			state.rainColumns.addAll(rain[row]);
			state.snowColumns.addAll(snow[row]);
		}
		return true;
	}
}
