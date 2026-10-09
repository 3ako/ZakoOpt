package zako.opt.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudInvoker {
	@Invoker("extractCameraOverlays")
	void zakoopt$extractCameraOverlays(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

	@Invoker("extractCrosshair")
	void zakoopt$extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
