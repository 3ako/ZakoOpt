package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.entity.CarriageCouplingRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.TrainCouplings;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = CarriageCouplingRenderer.class, remap = false)
public class CreateCouplingMixin {
	@Inject(require = 0, method = "renderAll", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$couplings(PoseStack ms, MultiBufferSource buffer, Vec3 camera, CallbackInfo ci) {
		if (Features.CREATE_COUPLINGS.on && TrainCouplings.render(ms, buffer, camera)) {
			ci.cancel();
		}
	}
}
