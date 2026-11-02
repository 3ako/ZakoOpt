package zako.opt.mixin.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.block.FactoryPaths;
import zako.opt.feature.Features;

@Pseudo
@Mixin(value = FactoryPanelRenderer.class, remap = false)
public class CreateFactoryPanelMixin {
	@Inject(require = 0, method = "renderPath", at = @At("HEAD"), cancellable = true)
	private static void zakoopt$cachedPath(FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks, PoseStack ms, MultiBufferSource buffer,
										   int light, int overlay, CallbackInfo ci) {
		if (Features.CREATE_PATHS.on && !FactoryPaths.recording && FactoryPaths.render(behaviour, connection, partialTicks, ms, buffer, light, overlay,
				recorder -> FactoryPanelRenderer.renderPath(behaviour, connection, partialTicks, new PoseStack(), recorder, light, overlay))) {
			ci.cancel();
		}
	}
}
