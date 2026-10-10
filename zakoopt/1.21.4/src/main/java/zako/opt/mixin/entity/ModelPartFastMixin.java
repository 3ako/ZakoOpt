package zako.opt.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.entity.FastCubes;

import java.util.List;

// a lower priority than VulkanMod's own compile hook, so this one runs first and cancels it
@Mixin(value = ModelPart.class, priority = 500)
public class ModelPartFastMixin {
	@Shadow
	@Final
	private List<ModelPart.Cube> cubes;

	@Inject(method = "compile", at = @At("HEAD"), cancellable = true)
	private void zakoopt$fastCubes(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color, CallbackInfo ci) {
		if (ZakoOptConfig.fastCubes() && FastCubes.render(cubes, pose, consumer, light, overlay, color)) {
			ci.cancel();
		}
	}
}
