package remodik.opt.mixin.entity;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

// every model resets every part before posing it, every frame: the part list is immutable and its for-each made an iterator
// per model per frame (5% of the render thread's garbage on a farm). Same order, same calls, by index
@Mixin(Model.class)
public class ModelResetPoseMixin {
	@Shadow
	@Final
	private List<ModelPart> allParts;

	@Overwrite
	public final void resetPose() {
		List<ModelPart> parts = allParts;
		for (int i = 0, n = parts.size(); i < n; i++) {
			parts.get(i).resetPose();
		}
	}
}
