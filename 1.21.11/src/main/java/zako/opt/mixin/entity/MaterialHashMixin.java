package zako.opt.mixin.entity;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

// sprite lookups hash a Material per chest, sign and item per frame, and Objects.hash made a varargs array every time.
// Same value as Objects.hash(atlasLocation, texture), worked out once: both fields are final
@Mixin(Material.class)
public class MaterialHashMixin {
	@Shadow
	@Final
	private Identifier atlasLocation;
	@Shadow
	@Final
	private Identifier texture;
	@Unique
	private int zakoopt$hash;
	@Unique
	private boolean zakoopt$hashed;

	@Overwrite
	public int hashCode() {
		if (!zakoopt$hashed) {
			zakoopt$hash = 31 * (31 + Objects.hashCode(atlasLocation)) + Objects.hashCode(texture);
			zakoopt$hashed = true;
		}
		return zakoopt$hash;
	}
}
