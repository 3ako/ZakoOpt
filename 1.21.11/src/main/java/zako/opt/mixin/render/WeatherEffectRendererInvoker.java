package zako.opt.mixin.render;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WeatherEffectRenderer.class)
public interface WeatherEffectRendererInvoker {
	@Invoker("createRainColumnInstance")
	WeatherEffectRenderer.ColumnInstance zakoopt$rain(RandomSource random, int ticks, int x, int bottomY, int topY, int z, int light, float partialTick);

	@Invoker("createSnowColumnInstance")
	WeatherEffectRenderer.ColumnInstance zakoopt$snow(RandomSource random, int ticks, int x, int bottomY, int topY, int z, int light, float partialTick);

	@Invoker("getPrecipitationAt")
	Biome.Precipitation zakoopt$precipitation(Level level, BlockPos pos);
}
