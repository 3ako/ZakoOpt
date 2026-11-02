package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoseStack.Pose.class)
public interface PoseAccessor {
	@Accessor("trustedNormals")
	boolean zakoopt$trustedNormals();

	@Accessor("trustedNormals")
	void zakoopt$trustedNormals(boolean trusted);
}
