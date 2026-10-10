package zako.opt.particle;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.Function;

// the animate tick looks up a (fuzzed) biome for ~1300 random blocks a tick only to ask for its ambient particles, which
// only a few nether biomes have; the fuzz never reaches past the neighbouring chunk, so a chunk whose 3x3 area holds
// no such biome can answer with any particle-free biome
@UtilityClass
public class AmbientBiomes {
	private final Long2BooleanOpenHashMap NEARBY = new Long2BooleanOpenHashMap();
	private Holder<Biome> plain;

	public Holder<Biome> biome(ClientLevel level, BlockPos pos, Function<BlockPos, Holder<Biome>> original) {
		if (plain != null) {
			int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
			long key = ChunkPos.asLong(cx, cz);
			if (!NEARBY.containsKey(key)) {
				Boolean nearby = scan(level, cx, cz);
				if (nearby == null) {
					return remember(original.apply(pos));
				}
				NEARBY.put(key, nearby.booleanValue());
			}
			if (!NEARBY.get(key)) {
				return plain;
			}
		}
		return remember(original.apply(pos));
	}

	// a chunk that (re)appears changes the answer for itself and the eight areas around it
	public void invalidate(int cx, int cz) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				NEARBY.remove(ChunkPos.asLong(cx + dx, cz + dz));
			}
		}
	}

	private Holder<Biome> remember(Holder<Biome> biome) {
		if (plain == null && biome.value().getAmbientParticle().isEmpty()) {
			plain = biome;
		}
		return biome;
	}

	// null: a chunk of the area is not loaded yet
	private Boolean scan(ClientLevel level, int cx, int cz) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(cx + dx, cz + dz, false);
				if (chunk == null) {
					return null;
				}
				for (LevelChunkSection section : chunk.getSections()) {
					if (section.getBiomes().maybeHas(b -> b.value().getAmbientParticle().isPresent())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
