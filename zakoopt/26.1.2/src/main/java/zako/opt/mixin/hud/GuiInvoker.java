package zako.opt.mixin.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiInvoker {
	@Invoker("extractCameraOverlays")
	void zakoopt$extractCameraOverlays(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

	@Invoker("extractCrosshair")
	void zakoopt$extractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
