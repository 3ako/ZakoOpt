package zako.opt.gl;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;

// Sodium 0.7 and newer count a section's geometry in vertices
@UtilityClass
public class SodiumSections {
	public long count(long data, int facing) {
		return SectionRenderDataUnsafe.getVertexCount(data, facing);
	}
}
