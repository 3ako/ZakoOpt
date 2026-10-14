package zako.opt.gl;

import lombok.experimental.UtilityClass;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataUnsafe;

// Sodium 0.6 counts a section's geometry in elements
@UtilityClass
public class SodiumSections {
	public long count(long data, int facing) {
		return SectionRenderDataUnsafe.getElementCount(data, facing);
	}
}
