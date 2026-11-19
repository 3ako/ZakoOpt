package zako.opt.mixin.chunk;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSkyLightSources.class)
public interface ChunkSkyLightSourcesAccessor {
	@Accessor("heightmap")
	BitStorage zakoopt$heightmap();

	@Accessor("minY")
	int zakoopt$minY();
}
