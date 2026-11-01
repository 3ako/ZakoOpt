package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.TrainDoors;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = SlidingDoorRenderer.class, remap = false)
public class CreateDoorMixin {
	@Inject(require = 0, method = "renderSafe(Lcom/simibubi/create/content/decoration/slidingDoor/SlidingDoorBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"), cancellable = true)
	private void zakoopt$doors(SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
		if (Features.CREATE_DOORS.on && TrainDoors.render((SlidingDoorRenderer) (Object) this, be, partialTicks, ms, buffer, light, overlay)) {
			ci.cancel();
		}
	}
}
